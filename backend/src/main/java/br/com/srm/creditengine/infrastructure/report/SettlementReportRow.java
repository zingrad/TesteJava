package br.com.srm.creditengine.infrastructure.report;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SettlementReportRow(
        String reference,
        String assignorTaxId,
        String assignorLegalName,
        String faceCurrency,
        String paymentCurrency,
        String status,
        BigDecimal baseMonthlyRate,
        BigDecimal exchangeRate,
        BigDecimal totalFaceValue,
        BigDecimal totalPresentValue,
        BigDecimal totalNetAmount,
        int itemCount,
        OffsetDateTime requestedAt,
        OffsetDateTime settledAt) {
}
