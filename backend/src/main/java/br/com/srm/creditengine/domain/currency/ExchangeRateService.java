package br.com.srm.creditengine.domain.currency;

import br.com.srm.creditengine.domain.exception.BusinessRuleException;
import br.com.srm.creditengine.infrastructure.currency.ExchangeRateProvider;
import br.com.srm.creditengine.infrastructure.currency.ExchangeRateRepository;
import br.com.srm.creditengine.infrastructure.currency.ProviderQuote;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExchangeRateService {

    private static final Limit ONLY_LATEST = Limit.of(1);

    private final ExchangeRateRepository rates;
    private final CurrencyService currencies;
    private final ExchangeRateProvider provider;
    private final Clock clock;

    ExchangeRateService(ExchangeRateRepository rates, CurrencyService currencies,
            ExchangeRateProvider provider, Clock clock) {
        this.rates = rates;
        this.currencies = currencies;
        this.provider = provider;
        this.clock = clock;
    }

    @Transactional
    public ExchangeRate register(String base, String quote, BigDecimal rate, OffsetDateTime effectiveAt) {
        return save(base, quote, rate, effectiveAt == null ? now() : effectiveAt, RateSource.MANUAL);
    }

    @Transactional
    public List<ExchangeRate> syncFromProvider() {
        OffsetDateTime moment = now();
        List<ProviderQuote> quotes = provider.latestQuotes();
        if (quotes.isEmpty()) {
            throw new BusinessRuleException("FX_PROVIDER_EMPTY", "O provedor externo não retornou cotações.");
        }
        return quotes.stream()
                .map(quote -> save(quote.baseCurrency(), quote.quoteCurrency(), quote.rate(),
                        moment, RateSource.PROVIDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExchangeRate currentRate(String base, String quote) {
        String normalizedBase = normalize(base);
        String normalizedQuote = normalize(quote);
        return rates.findEffectiveAt(normalizedBase, normalizedQuote, now(), ONLY_LATEST)
                .orElseThrow(() -> new BusinessRuleException("FX_RATE_UNAVAILABLE",
                        "Não existe cotação vigente para o par %s/%s.".formatted(normalizedBase, normalizedQuote)));
    }

    @Transactional(readOnly = true)
    public List<ExchangeRate> history(String base, String quote, int limit) {
        return rates.findHistory(normalize(base), normalize(quote), Limit.of(limit));
    }

    /**
     * Conversão entre moedas iguais e identidade: não ha cotação a buscar nem arredondamento a aplicar
     * além do ajuste de escala da própria moeda.
     */
    @Transactional(readOnly = true)
    public ConvertedAmount convert(BigDecimal amount, String base, String quote) {
        String normalizedBase = normalize(base);
        String normalizedQuote = normalize(quote);

        if (normalizedBase.equals(normalizedQuote)) {
            Currency currency = currencies.require(normalizedBase);
            return new ConvertedAmount(
                    amount.setScale(currency.minorUnit(), RoundingMode.HALF_EVEN), BigDecimal.ONE, null);
        }

        ExchangeRate rate = currentRate(normalizedBase, normalizedQuote);
        return new ConvertedAmount(rate.convert(amount), rate.rate(), rate);
    }

    private ExchangeRate save(String base, String quote, BigDecimal rate,
            OffsetDateTime effectiveAt, RateSource source) {

        String normalizedBase = normalize(base);
        String normalizedQuote = normalize(quote);
        if (normalizedBase.equals(normalizedQuote)) {
            throw new BusinessRuleException("FX_SAME_CURRENCY",
                    "Não faz sentido cotar uma moeda contra ela mesma.");
        }
        return rates.save(new ExchangeRate(
                currencies.require(normalizedBase), currencies.require(normalizedQuote),
                rate, effectiveAt, source));
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private static String normalize(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
