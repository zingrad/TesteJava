package br.com.srm.creditengine.infrastructure.report;

import java.time.LocalDate;

/**
 * Recorte do extrato. As datas sao inclusivas nas duas pontas e interpretadas em UTC, que e o fuso
 * em que {@code settled_at} e gravado.
 */
public record SettlementReportFilter(
        LocalDate from,
        LocalDate to,
        String assignorTaxId,
        String paymentCurrency,
        String status,
        SettlementReportSort sort,
        SortDirection direction,
        int page,
        int size) {

    public enum SortDirection {
        ASC, DESC
    }

    int offset() {
        return page * size;
    }
}
