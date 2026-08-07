package br.com.srm.creditengine.domain.pricing;

import br.com.srm.creditengine.domain.currency.CurrencyService;
import br.com.srm.creditengine.domain.currency.ExchangeRate;
import br.com.srm.creditengine.domain.currency.ExchangeRateService;
import br.com.srm.creditengine.domain.exception.BusinessRuleException;
import ch.obermuhlner.math.big.BigDecimalMath;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PricingService {

    /**
     * Prazo em meses comerciais de 30 dias, convenção usual em operações de desconto no mercado
     * brasileiro. Um título de 45 dias vale 1,5 mês, e não 1 mês arredondado.
     */
    private static final BigDecimal DAYS_IN_COMMERCIAL_MONTH = new BigDecimal("30");

    /**
     * Precisão intermediaria bem acima da escala de armazenamento: o arredondamento acontece uma
     * única vez, no valor final de cada título, e não se acumula ao longo da potenciação.
     */
    private static final MathContext CALCULATION = new MathContext(24, RoundingMode.HALF_EVEN);

    private final PricingStrategyRegistry strategies;
    private final ReceivableTypeService receivableTypes;
    private final ExchangeRateService exchangeRates;
    private final CurrencyService currencies;
    private final Clock clock;

    PricingService(PricingStrategyRegistry strategies, ReceivableTypeService receivableTypes,
            ExchangeRateService exchangeRates, CurrencyService currencies, Clock clock) {
        this.strategies = strategies;
        this.receivableTypes = receivableTypes;
        this.exchangeRates = exchangeRates;
        this.currencies = currencies;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PricedBatch price(PricingOrder order) {
        if (order.items().isEmpty()) {
            throw new BusinessRuleException("EMPTY_BATCH", "O lote não contém nenhum título.");
        }

        String faceCurrency = normalize(order.faceCurrency());
        String paymentCurrency = normalize(order.paymentCurrency());
        int faceScale = currencies.require(faceCurrency).minorUnit();

        ExchangeRate appliedRate = faceCurrency.equals(paymentCurrency)
                ? null
                : exchangeRates.currentRate(faceCurrency, paymentCurrency);

        LocalDate valuationDate = LocalDate.now(clock);
        Map<String, ReceivableType> resolvedTypes = new HashMap<>();
        Set<String> documents = new HashSet<>();
        List<PricedReceivable> priced = new ArrayList<>(order.items().size());

        for (PricingOrder.Receivable item : order.items()) {
            if (!documents.add(item.documentNumber())) {
                throw new BusinessRuleException("DUPLICATE_DOCUMENT",
                        "O documento %s aparece mais de uma vez no lote.".formatted(item.documentNumber()));
            }
            ReceivableType type = resolvedTypes.computeIfAbsent(
                    normalize(item.receivableTypeCode()), receivableTypes::requireActive);

            priced.add(price(item, type, order.baseMonthlyRate(), valuationDate, faceScale, appliedRate));
        }

        return new PricedBatch(
                valuationDate,
                faceCurrency,
                paymentCurrency,
                order.baseMonthlyRate(),
                appliedRate == null ? BigDecimal.ONE : appliedRate.rate(),
                appliedRate,
                total(priced, PricedReceivable::faceValue),
                total(priced, PricedReceivable::presentValue),
                total(priced, PricedReceivable::netAmount),
                List.copyOf(priced));
    }

    private PricedReceivable price(PricingOrder.Receivable item, ReceivableType type,
            BigDecimal baseMonthlyRate, LocalDate valuationDate, int faceScale, ExchangeRate appliedRate) {

        if (!item.dueDate().isAfter(item.issueDate())) {
            throw new BusinessRuleException("INVALID_TERM",
                    "O vencimento do documento %s não e posterior a emissão.".formatted(item.documentNumber()));
        }

        int termDays = (int) ChronoUnit.DAYS.between(valuationDate, item.dueDate());
        if (termDays <= 0) {
            throw new BusinessRuleException("RECEIVABLE_NOT_DISCOUNTABLE",
                    "O documento %s vence em %s e não tem prazo a descontar."
                            .formatted(item.documentNumber(), item.dueDate()));
        }

        BigDecimal monthlyRate = strategies.forType(type.code())
                .monthlyDiscountRate(new PricingParameters(baseMonthlyRate, type, termDays));

        BigDecimal presentValue = presentValue(item.faceValue(), monthlyRate, termDays, faceScale);
        BigDecimal netAmount = appliedRate == null ? presentValue : appliedRate.convert(presentValue);

        return new PricedReceivable(
                item.documentNumber(),
                type.code(),
                item.faceValue(),
                item.issueDate(),
                item.dueDate(),
                termDays,
                type.monthlySpread(),
                monthlyRate,
                presentValue,
                netAmount);
    }

    /**
     * Valor Presente = Valor de Face / (1 + taxa mensal) ^ (prazo em meses).
     */
    private BigDecimal presentValue(BigDecimal faceValue, BigDecimal monthlyRate, int termDays, int scale) {
        BigDecimal growthFactor = BigDecimal.ONE.add(monthlyRate);
        if (growthFactor.signum() <= 0) {
            throw new BusinessRuleException("INVALID_DISCOUNT_RATE",
                    "A taxa de desconto resultante torna o cálculo indefinido.");
        }

        BigDecimal termInMonths = new BigDecimal(termDays).divide(DAYS_IN_COMMERCIAL_MONTH, CALCULATION);
        BigDecimal discountFactor = BigDecimalMath.pow(growthFactor, termInMonths, CALCULATION);

        return faceValue.divide(discountFactor, CALCULATION).setScale(scale, RoundingMode.HALF_EVEN);
    }

    /**
     * O total do lote soma os títulos já arredondados, em vez de arredondar a soma. E o único jeito
     * de o cabeçalho fechar com a linha a linha do extrato, que é o que a auditoria confere.
     */
    private static BigDecimal total(List<PricedReceivable> items,
            Function<PricedReceivable, BigDecimal> field) {

        return items.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
