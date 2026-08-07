package br.com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PricingOrder(
        String faceCurrency,
        String paymentCurrency,
        BigDecimal baseMonthlyRate,
        List<Receivable> items) {

    public record Receivable(
            String documentNumber,
            String receivableTypeCode,
            BigDecimal faceValue,
            LocalDate issueDate,
            LocalDate dueDate) {
    }
}
