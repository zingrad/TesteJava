package br.com.srm.creditengine.domain.currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.srm.creditengine.domain.exception.BusinessRuleException;
import br.com.srm.creditengine.infrastructure.currency.ExchangeRateProvider;
import br.com.srm.creditengine.infrastructure.currency.ExchangeRateRepository;
import br.com.srm.creditengine.infrastructure.currency.ProviderQuote;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExchangeRateServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 3, 10, 12, 0, 0, 0, ZoneOffset.UTC);

    @Mock
    private ExchangeRateRepository rates;

    @Mock
    private CurrencyService currencies;

    @Mock
    private ExchangeRateProvider provider;

    private ExchangeRateService service;
    private Currency brl;
    private Currency usd;

    @BeforeEach
    void setUp() {
        brl = currency("BRL", "Real brasileiro", (short) 2);
        usd = currency("USD", "Dólar norte-americano", (short) 2);

        when(currencies.require("BRL")).thenReturn(brl);
        when(currencies.require("USD")).thenReturn(usd);
        when(rates.save(any(ExchangeRate.class))).thenAnswer(call -> call.getArgument(0));

        service = new ExchangeRateService(rates, currencies, provider,
                Clock.fixed(NOW.toInstant(), ZoneOffset.UTC));
    }

    @Test
    void normalizesCurrencyCodesBeforePersisting() {
        service.register(" usd ", "brl", new BigDecimal("5.42"), null);

        assertThat(persistedRate().baseCurrency().code()).isEqualTo("USD");
        assertThat(persistedRate().quoteCurrency().code()).isEqualTo("BRL");
    }

    @Test
    void defaultsEffectiveMomentToTheInjectedClock() {
        service.register("USD", "BRL", new BigDecimal("5.42"), null);

        assertThat(persistedRate().effectiveAt()).isEqualTo(NOW);
        assertThat(persistedRate().source()).isEqualTo(RateSource.MANUAL);
    }

    @Test
    void keepsTheEffectiveMomentWhenTheOperatorInformsIt() {
        OffsetDateTime backdated = NOW.minusDays(3);

        service.register("USD", "BRL", new BigDecimal("5.42"), backdated);

        assertThat(persistedRate().effectiveAt()).isEqualTo(backdated);
    }

    @Test
    void rejectsQuotingACurrencyAgainstItself() {
        assertThatThrownBy(() -> service.register("USD", "usd", new BigDecimal("1"), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ela mesma");

        verify(rates, never()).save(any());
    }

    @Test
    void currentRateLookupIsBoundedByTheCurrentMoment() {
        stubEffectiveRate(new BigDecimal("5.42000000"));

        service.currentRate("usd", " brl ");

        verify(rates).findEffectiveAt(eq("USD"), eq("BRL"), eq(NOW), any());
    }

    @Test
    void failsWithBusinessCodeWhenPairHasNoEffectiveQuote() {
        when(rates.findEffectiveAt(eq("USD"), eq("BRL"), any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.currentRate("USD", "BRL"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting(thrown -> ((BusinessRuleException) thrown).code())
                .isEqualTo("FX_RATE_UNAVAILABLE");
    }

    @Test
    void convertsUsingTheEffectiveRateRoundedToTheQuoteCurrencyScale() {
        stubEffectiveRate(new BigDecimal("5.42150000"));

        ConvertedAmount converted = service.convert(new BigDecimal("1000.00"), "USD", "BRL");

        assertThat(converted.amount()).isEqualByComparingTo("5421.50");
        assertThat(converted.rate()).isEqualByComparingTo("5.42150000");
        assertThat(converted.crossCurrency()).isTrue();
    }

    /**
     * Meia unidade exata é o único caso em que HALF_EVEN e HALF_UP divergem: 10.005 desce para 10.00
     * porque 0 e par, enquanto 10.015 sobe para 10.02. E o comportamento desejado — arredondar sempre
     * para cima introduziria viés de alta no caixa ao longo de muitas conversoes.
     */
    @Test
    void breaksExactHalvesTowardsTheEvenDigit() {
        stubEffectiveRate(new BigDecimal("1.00050000"));
        assertThat(service.convert(new BigDecimal("10.00"), "USD", "BRL").amount())
                .isEqualByComparingTo("10.00");

        stubEffectiveRate(new BigDecimal("1.00150000"));
        assertThat(service.convert(new BigDecimal("10.00"), "USD", "BRL").amount())
                .isEqualByComparingTo("10.02");
    }

    @Test
    void conversionBetweenTheSameCurrencyNeitherLooksUpARateNorChangesTheAmount() {
        ConvertedAmount converted = service.convert(new BigDecimal("1234.5"), "BRL", "brl");

        assertThat(converted.amount()).isEqualByComparingTo("1234.50");
        assertThat(converted.amount().scale()).isEqualTo(2);
        assertThat(converted.rate()).isEqualByComparingTo("1");
        assertThat(converted.crossCurrency()).isFalse();
        verify(rates, never()).findEffectiveAt(any(), any(), any(), any());
    }

    @Test
    void providerSyncStampsEverySavedQuoteWithTheSameMoment() {
        when(provider.latestQuotes()).thenReturn(List.of(
                new ProviderQuote("USD", "BRL", new BigDecimal("5.43")),
                new ProviderQuote("BRL", "USD", new BigDecimal("0.18416206"))));

        List<ExchangeRate> synced = service.syncFromProvider();

        assertThat(synced).hasSize(2).allSatisfy(rate -> {
            assertThat(rate.effectiveAt()).isEqualTo(NOW);
            assertThat(rate.source()).isEqualTo(RateSource.PROVIDER);
        });
    }

    @Test
    void providerSyncFailsLoudlyWhenNothingComesBack() {
        when(provider.latestQuotes()).thenReturn(List.of());

        assertThatThrownBy(() -> service.syncFromProvider())
                .isInstanceOf(BusinessRuleException.class)
                .extracting(thrown -> ((BusinessRuleException) thrown).code())
                .isEqualTo("FX_PROVIDER_EMPTY");
    }

    private ExchangeRate persistedRate() {
        ArgumentCaptor<ExchangeRate> saved = ArgumentCaptor.forClass(ExchangeRate.class);
        verify(rates).save(saved.capture());
        return saved.getValue();
    }

    private void stubEffectiveRate(BigDecimal value) {
        when(rates.findEffectiveAt(eq("USD"), eq("BRL"), any(), any()))
                .thenReturn(Optional.of(new ExchangeRate(usd, brl, value, NOW, RateSource.MANUAL)));
    }

    private static Currency currency(String code, String name, short minorUnit) {
        Currency currency = new Currency();
        ReflectionTestUtils.setField(currency, "code", code);
        ReflectionTestUtils.setField(currency, "name", name);
        ReflectionTestUtils.setField(currency, "minorUnit", minorUnit);
        return currency;
    }
}
