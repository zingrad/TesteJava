package br.com.srm.creditengine.api.currency;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.srm.creditengine.api.error.ApiErrorHandler;
import br.com.srm.creditengine.domain.currency.Currency;
import br.com.srm.creditengine.domain.currency.ExchangeRate;
import br.com.srm.creditengine.domain.currency.ExchangeRateService;
import br.com.srm.creditengine.domain.currency.RateSource;
import br.com.srm.creditengine.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ExchangeRateController.class)
@Import(ApiErrorHandler.class)
class ExchangeRateControllerTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 3, 10, 12, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateService service;

    @Test
    void registeringAQuoteAnswersCreatedWithTheResourceLocation() throws Exception {
        when(service.register(eq("USD"), eq("BRL"), any(), any())).thenReturn(rate(7L, "5.42"));

        mockMvc.perform(post("/api/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseCurrency": "USD", "quoteCurrency": "BRL", "rate": 5.42}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/exchange-rates/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.baseCurrency").value("USD"))
                .andExpect(jsonPath("$.rate").value(5.42))
                .andExpect(jsonPath("$.source").value("MANUAL"));
    }

    @Test
    void rejectsANonPositiveRateBeforeReachingTheDomain() throws Exception {
        mockMvc.perform(post("/api/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseCurrency": "USD", "quoteCurrency": "BRL", "rate": 0}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].field").value("rate"))
                .andExpect(jsonPath("$.violations[0].message").value("deve ser maior que zero"));

        verify(service, never()).register(any(), any(), any(), any());
    }

    @Test
    void rejectsACurrencyCodeThatIsNotIso4217() throws Exception {
        mockMvc.perform(post("/api/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseCurrency": "DOLAR", "quoteCurrency": "BRL", "rate": 5.42}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("baseCurrency"))
                .andExpect(jsonPath("$.violations[0].message").value("deve ser um código ISO 4217 de três letras"));
    }

    @Test
    void rejectsARateWithMoreDecimalsThanTheSchemaHolds() throws Exception {
        mockMvc.perform(post("/api/exchange-rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"baseCurrency": "USD", "quoteCurrency": "BRL", "rate": 5.4212345678}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("rate"))
                .andExpect(jsonPath("$.violations[0].message").value("aceita no máximo 8 casas decimais"));
    }

    @Test
    void missingQuoteForThePairSurfacesAsUnprocessableEntity() throws Exception {
        when(service.currentRate("USD", "JPY"))
                .thenThrow(new BusinessRuleException("FX_RATE_UNAVAILABLE", "Não existe cotação vigente."));

        mockMvc.perform(get("/api/exchange-rates/current").param("base", "USD").param("quote", "JPY"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FX_RATE_UNAVAILABLE"));
    }

    @Test
    void rejectsAHistoryLimitOutsideTheAcceptedRange() throws Exception {
        mockMvc.perform(get("/api/exchange-rates")
                        .param("base", "USD").param("quote", "BRL").param("limit", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        verify(service, never()).history(any(), any(), anyInt());
    }

    private static ExchangeRate rate(Long id, String value) {
        ExchangeRate rate = new ExchangeRate(
                currency("USD"), currency("BRL"), new BigDecimal(value), NOW, RateSource.MANUAL);
        ReflectionTestUtils.setField(rate, "id", id);
        return rate;
    }

    private static Currency currency(String code) {
        Currency currency = BeanUtils.instantiateClass(Currency.class);
        ReflectionTestUtils.setField(currency, "code", code);
        ReflectionTestUtils.setField(currency, "name", code);
        ReflectionTestUtils.setField(currency, "minorUnit", (short) 2);
        return currency;
    }
}
