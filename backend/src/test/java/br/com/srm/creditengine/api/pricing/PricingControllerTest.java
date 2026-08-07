package br.com.srm.creditengine.api.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.srm.creditengine.api.error.ApiErrorHandler;
import br.com.srm.creditengine.domain.currency.Currency;
import br.com.srm.creditengine.domain.currency.ExchangeRate;
import br.com.srm.creditengine.domain.currency.RateSource;
import br.com.srm.creditengine.domain.exception.BusinessRuleException;
import br.com.srm.creditengine.domain.pricing.PricedBatch;
import br.com.srm.creditengine.domain.pricing.PricedReceivable;
import br.com.srm.creditengine.domain.pricing.PricingOrder;
import br.com.srm.creditengine.domain.pricing.PricingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = PricingController.class)
@Import(ApiErrorHandler.class)
class PricingControllerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PricingService pricing;

    @Test
    void returnsTheBreakdownAndTheBatchTotals() throws Exception {
        when(pricing.price(any())).thenReturn(batch());

        mockMvc.perform(simulate("""
                {
                  "faceCurrency": "BRL",
                  "paymentCurrency": "USD",
                  "baseMonthlyRate": 0.01,
                  "items": [
                    {"documentNumber": "DUP-1", "receivableTypeCode": "MERCANTILE_INVOICE",
                     "faceValue": 10000.00, "issueDate": "2026-03-05", "dueDate": "2026-05-09"}
                  ]
                }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.crossCurrency").value(true))
                .andExpect(jsonPath("$.exchangeRate").value(0.18450000))
                .andExpect(jsonPath("$.totalPresentValue").value(9518.14))
                .andExpect(jsonPath("$.totalNetAmount").value(1756.10))
                .andExpect(jsonPath("$.items[0].termDays").value(60))
                .andExpect(jsonPath("$.items[0].appliedSpread").value(0.015));
    }

    @Test
    void forwardsTheOrderToTheDomainWithoutReinterpretingIt() throws Exception {
        when(pricing.price(any())).thenReturn(batch());

        mockMvc.perform(simulate("""
                {
                  "faceCurrency": "BRL", "paymentCurrency": "USD", "baseMonthlyRate": 0.0125,
                  "items": [
                    {"documentNumber": "  DUP-9  ", "receivableTypeCode": "POST_DATED_CHECK",
                     "faceValue": 500.55, "issueDate": "2026-03-01", "dueDate": "2026-06-01"}
                  ]
                }"""))
                .andExpect(status().isOk());

        ArgumentCaptor<PricingOrder> order = ArgumentCaptor.forClass(PricingOrder.class);
        verify(pricing).price(order.capture());

        PricingOrder captured = order.getValue();
        assertThat(captured.baseMonthlyRate())
                .isEqualByComparingTo("0.0125");
        assertThat(captured.items().getFirst().documentNumber())
                .isEqualTo("DUP-9");
    }

    @Test
    void rejectsAnEmptyBatchBeforeReachingTheDomain() throws Exception {
        mockMvc.perform(simulate("""
                {"faceCurrency": "BRL", "paymentCurrency": "BRL", "baseMonthlyRate": 0.01, "items": []}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.violations[0].message").value("o lote precisa ter ao menos um título"));

        verify(pricing, never()).price(any());
    }

    @Test
    void reportsTheOffendingItemPositionOnNestedValidation() throws Exception {
        mockMvc.perform(simulate("""
                {
                  "faceCurrency": "BRL", "paymentCurrency": "BRL", "baseMonthlyRate": 0.01,
                  "items": [
                    {"documentNumber": "DUP-1", "receivableTypeCode": "MERCANTILE_INVOICE",
                     "faceValue": 100.00, "issueDate": "2026-03-05", "dueDate": "2026-05-09"},
                    {"documentNumber": "DUP-2", "receivableTypeCode": "MERCANTILE_INVOICE",
                     "faceValue": -5, "issueDate": "2026-03-05", "dueDate": "2026-05-09"}
                  ]
                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("items[1].faceValue"))
                .andExpect(jsonPath("$.violations[0].message").value("deve ser maior que zero"));
    }

    @Test
    void rejectsABaseRateAboveTheOperationalCeiling() throws Exception {
        mockMvc.perform(simulate("""
                {
                  "faceCurrency": "BRL", "paymentCurrency": "BRL", "baseMonthlyRate": 5,
                  "items": [
                    {"documentNumber": "DUP-1", "receivableTypeCode": "MERCANTILE_INVOICE",
                     "faceValue": 100.00, "issueDate": "2026-03-05", "dueDate": "2026-05-09"}
                  ]
                }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("baseMonthlyRate"));
    }

    @Test
    void aReceivableAlreadyDueSurfacesAsUnprocessableEntity() throws Exception {
        when(pricing.price(any())).thenThrow(new BusinessRuleException(
                "RECEIVABLE_NOT_DISCOUNTABLE", "O documento DUP-1 vence em 2026-03-10."));

        mockMvc.perform(simulate("""
                {
                  "faceCurrency": "BRL", "paymentCurrency": "BRL", "baseMonthlyRate": 0.01,
                  "items": [
                    {"documentNumber": "DUP-1", "receivableTypeCode": "MERCANTILE_INVOICE",
                     "faceValue": 100.00, "issueDate": "2026-03-05", "dueDate": "2026-03-10"}
                  ]
                }"""))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("RECEIVABLE_NOT_DISCOUNTABLE"));
    }

    private static MockHttpServletRequestBuilder simulate(
            String body) {
        return post("/api/pricing/simulate").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private static PricedBatch batch() {
        PricedReceivable item = new PricedReceivable(
                "DUP-1", "MERCANTILE_INVOICE", new BigDecimal("10000.00"),
                TODAY.minusDays(5), TODAY.plusDays(60), 60,
                new BigDecimal("0.015"), new BigDecimal("0.025"),
                new BigDecimal("9518.14"), new BigDecimal("1756.10"));

        return new PricedBatch(TODAY, "BRL", "USD", new BigDecimal("0.01"),
                new BigDecimal("0.18450000"), appliedRate(),
                new BigDecimal("10000.00"), new BigDecimal("9518.14"), new BigDecimal("1756.10"),
                List.of(item));
    }

    private static ExchangeRate appliedRate() {
        return new ExchangeRate(currency("BRL"), currency("USD"), new BigDecimal("0.18450000"),
                TODAY.atStartOfDay().atOffset(ZoneOffset.UTC), RateSource.MANUAL);
    }

    private static Currency currency(String code) {
        Currency currency = BeanUtils.instantiateClass(Currency.class);
        ReflectionTestUtils.setField(currency, "code", code);
        ReflectionTestUtils.setField(currency, "name", code);
        ReflectionTestUtils.setField(currency, "minorUnit", (short) 2);
        return currency;
    }
}
