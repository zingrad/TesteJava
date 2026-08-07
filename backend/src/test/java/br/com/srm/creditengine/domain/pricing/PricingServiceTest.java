package br.com.srm.creditengine.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.srm.creditengine.domain.currency.Currency;
import br.com.srm.creditengine.domain.currency.CurrencyService;
import br.com.srm.creditengine.domain.currency.ExchangeRate;
import br.com.srm.creditengine.domain.currency.ExchangeRateService;
import br.com.srm.creditengine.domain.currency.RateSource;
import br.com.srm.creditengine.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PricingServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private static final String INVOICE = "MERCANTILE_INVOICE";
    private static final String CHECK = "POST_DATED_CHECK";

    @Mock
    private ReceivableTypeService receivableTypes;

    @Mock
    private ExchangeRateService exchangeRates;

    @Mock
    private CurrencyService currencies;

    private PricingService service;

    @BeforeEach
    void setUp() {
        when(currencies.require(any())).thenReturn(currency("BRL", (short) 2));
        when(receivableTypes.requireActive(INVOICE)).thenReturn(receivableType(INVOICE, "0.015"));
        when(receivableTypes.requireActive(CHECK)).thenReturn(receivableType(CHECK, "0.025"));

        PricingStrategyRegistry registry = new PricingStrategyRegistry(
                List.of(new MercantileInvoiceStrategy(), new PostDatedCheckStrategy()));

        service = new PricingService(registry, receivableTypes, exchangeRates, currencies,
                Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC));
    }

    /**
     * 10.000 / (1 + 0,01 + 0,015)^2 = 10.000 / 1,050625 = 9.518,14 (2 meses exatos, 60 dias).
     */
    @Test
    void discountsAWholeNumberOfCommercialMonths() {
        PricedBatch batch = price("0.01", receivable("DUP-1", INVOICE, "10000.00", TODAY.plusDays(60)));

        PricedReceivable item = batch.items().getFirst();
        assertThat(item.termDays()).isEqualTo(60);
        assertThat(item.monthlyDiscountRate()).isEqualByComparingTo("0.025");
        assertThat(item.presentValue()).isEqualByComparingTo("9518.14");
    }

    /**
     * 45 dias sao 1,5 mes comercial, e nao 1 ou 2: o expoente e fracionario.
     * 10.000 / 1,025^1,5 = 9.636,39.
     */
    @Test
    void discountsAFractionalTermInsteadOfRoundingTheMonths() {
        PricedBatch batch = price("0.01", receivable("DUP-1", INVOICE, "10000.00", TODAY.plusDays(45)));

        PricedReceivable item = batch.items().getFirst();
        assertThat(item.termDays()).isEqualTo(45);
        assertThat(item.presentValue()).isEqualByComparingTo("9636.39");
    }

    @Test
    void appliesTheSpreadOfEachReceivableType() {
        PricedBatch batch = price("0.01",
                receivable("DUP-1", INVOICE, "10000.00", TODAY.plusDays(30)),
                receivable("CHQ-1", CHECK, "10000.00", TODAY.plusDays(30)));

        PricedReceivable invoice = batch.items().getFirst();
        PricedReceivable check = batch.items().get(1);

        assertThat(invoice.appliedSpread()).isEqualByComparingTo("0.015");
        assertThat(check.appliedSpread()).isEqualByComparingTo("0.025");
        assertThat(invoice.monthlyDiscountRate()).isEqualByComparingTo("0.025");
        assertThat(check.monthlyDiscountRate()).isEqualByComparingTo("0.035");
        assertThat(check.presentValue()).isLessThan(invoice.presentValue());
    }

    @Test
    void aLongerTermIsWorthLessToday() {
        PricedBatch batch = price("0.01",
                receivable("DUP-1", INVOICE, "10000.00", TODAY.plusDays(30)),
                receivable("DUP-2", INVOICE, "10000.00", TODAY.plusDays(180)));

        assertThat(batch.items().get(1).presentValue())
                .isLessThan(batch.items().getFirst().presentValue());
    }

    @Test
    void aZeroRateLeavesThePresentValueEqualToTheFaceValue() {
        when(receivableTypes.requireActive(INVOICE)).thenReturn(receivableType(INVOICE, "0"));

        PricedBatch batch = price("0", receivable("DUP-1", INVOICE, "10000.00", TODAY.plusDays(90)));

        assertThat(batch.items().getFirst().presentValue()).isEqualByComparingTo("10000.00");
    }

    @Test
    void batchTotalsAreTheSumOfTheAlreadyRoundedItems() {
        PricedBatch batch = price("0.01",
                receivable("DUP-1", INVOICE, "3333.33", TODAY.plusDays(37)),
                receivable("DUP-2", INVOICE, "1111.11", TODAY.plusDays(53)),
                receivable("CHQ-1", CHECK, "7777.77", TODAY.plusDays(71)));

        BigDecimal sumOfItems = batch.items().stream()
                .map(PricedReceivable::presentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(batch.totalPresentValue()).isEqualByComparingTo(sumOfItems);
        assertThat(batch.totalFaceValue()).isEqualByComparingTo("12222.21");
    }

    @Test
    void aSingleCurrencyOperationNeitherLooksUpARateNorChangesTheNetAmount() {
        PricedBatch batch = price("0.01", receivable("DUP-1", INVOICE, "10000.00", TODAY.plusDays(60)));

        assertThat(batch.crossCurrency()).isFalse();
        assertThat(batch.exchangeRateValue()).isEqualByComparingTo("1");
        assertThat(batch.totalNetAmount()).isEqualByComparingTo(batch.totalPresentValue());
        verify(exchangeRates, never()).currentRate(any(), any());
    }

    /**
     * O cambio entra depois do desagio: 9.518,14 BRL x 0,18450000 = 1.756,10 USD.
     */
    @Test
    void crossCurrencyConvertsThePresentValueAtTheEnd() {
        when(exchangeRates.currentRate("BRL", "USD")).thenReturn(rate("0.18450000"));

        PricedBatch batch = service.price(new PricingOrder("BRL", "USD", new BigDecimal("0.01"),
                List.of(receivable("DUP-1", INVOICE, "10000.00", TODAY.plusDays(60)))));

        assertThat(batch.crossCurrency()).isTrue();
        assertThat(batch.exchangeRateValue()).isEqualByComparingTo("0.18450000");
        assertThat(batch.items().getFirst().presentValue()).isEqualByComparingTo("9518.14");
        assertThat(batch.items().getFirst().netAmount()).isEqualByComparingTo("1756.10");
        assertThat(batch.totalNetAmount()).isEqualByComparingTo("1756.10");
    }

    @Test
    void theRateIsResolvedOncePerBatchAndNotOncePerItem() {
        when(exchangeRates.currentRate("BRL", "USD")).thenReturn(rate("0.18450000"));

        service.price(new PricingOrder("BRL", "USD", new BigDecimal("0.01"), List.of(
                receivable("DUP-1", INVOICE, "1000.00", TODAY.plusDays(30)),
                receivable("DUP-2", INVOICE, "2000.00", TODAY.plusDays(60)),
                receivable("DUP-3", INVOICE, "3000.00", TODAY.plusDays(90)))));

        verify(exchangeRates).currentRate("BRL", "USD");
    }

    @Test
    void rejectsAReceivableThatIsAlreadyDue() {
        assertThatThrownBy(() -> price("0.01", receivable("DUP-1", INVOICE, "10000.00", TODAY)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(thrown -> ((BusinessRuleException) thrown).code())
                .isEqualTo("RECEIVABLE_NOT_DISCOUNTABLE");
    }

    @Test
    void rejectsAReceivableMaturingBeforeItWasIssued() {
        PricingOrder.Receivable inverted = new PricingOrder.Receivable(
                "DUP-1", INVOICE, new BigDecimal("10000.00"), TODAY.plusDays(90), TODAY.plusDays(30));

        assertThatThrownBy(() -> price("0.01", inverted))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(thrown -> ((BusinessRuleException) thrown).code())
                .isEqualTo("INVALID_TERM");
    }

    @Test
    void rejectsTheSameDocumentTwiceInTheSameBatch() {
        assertThatThrownBy(() -> price("0.01",
                receivable("DUP-1", INVOICE, "1000.00", TODAY.plusDays(30)),
                receivable("DUP-1", CHECK, "2000.00", TODAY.plusDays(60))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(thrown -> ((BusinessRuleException) thrown).code())
                .isEqualTo("DUPLICATE_DOCUMENT");
    }

    @Test
    void rejectsAnEmptyBatch() {
        assertThatThrownBy(() -> service.price(
                new PricingOrder("BRL", "BRL", new BigDecimal("0.01"), List.of())))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(thrown -> ((BusinessRuleException) thrown).code())
                .isEqualTo("EMPTY_BATCH");
    }

    @Test
    void refusesToPriceATypeThatHasNoStrategy() {
        when(receivableTypes.requireActive("EXPORT_NOTE"))
                .thenReturn(receivableType("EXPORT_NOTE", "0.02"));

        assertThatThrownBy(() -> price("0.01",
                receivable("EXP-1", "EXPORT_NOTE", "1000.00", TODAY.plusDays(30))))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(thrown -> ((BusinessRuleException) thrown).code())
                .isEqualTo("PRICING_STRATEGY_MISSING");
    }

    private PricedBatch price(String baseRate, PricingOrder.Receivable... items) {
        return service.price(new PricingOrder("BRL", "BRL", new BigDecimal(baseRate), List.of(items)));
    }

    private static PricingOrder.Receivable receivable(String document, String type, String faceValue,
            LocalDate dueDate) {
        return new PricingOrder.Receivable(
                document, type, new BigDecimal(faceValue), TODAY.minusDays(5), dueDate);
    }

    private static ReceivableType receivableType(String code, String spread) {
        ReceivableType type = BeanUtils.instantiateClass(ReceivableType.class);
        ReflectionTestUtils.setField(type, "code", code);
        ReflectionTestUtils.setField(type, "name", code);
        ReflectionTestUtils.setField(type, "monthlySpread", new BigDecimal(spread));
        ReflectionTestUtils.setField(type, "active", true);
        return type;
    }

    private static ExchangeRate rate(String value) {
        return new ExchangeRate(currency("BRL", (short) 2), currency("USD", (short) 2),
                new BigDecimal(value), OffsetDateTime.of(2026, 3, 10, 0, 0, 0, 0, ZoneOffset.UTC),
                RateSource.MANUAL);
    }

    private static Currency currency(String code, short minorUnit) {
        Currency currency = BeanUtils.instantiateClass(Currency.class);
        ReflectionTestUtils.setField(currency, "code", code);
        ReflectionTestUtils.setField(currency, "name", code);
        ReflectionTestUtils.setField(currency, "minorUnit", minorUnit);
        return currency;
    }
}
