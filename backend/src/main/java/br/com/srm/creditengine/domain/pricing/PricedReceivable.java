package br.com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PricedReceivable(
        String documentNumber,
        String receivableTypeCode,
        BigDecimal faceValue,
        LocalDate issueDate,
        LocalDate dueDate,
        int termDays,
        BigDecimal appliedSpread,
        BigDecimal monthlyDiscountRate,
        BigDecimal presentValue,
        BigDecimal netAmount) {
}
