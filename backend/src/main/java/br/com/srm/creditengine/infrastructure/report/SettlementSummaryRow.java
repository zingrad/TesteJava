package br.com.srm.creditengine.infrastructure.report;

import java.math.BigDecimal;

/**
 * Totais agrupados por par de moedas. Somar valores de moedas diferentes na mesma linha não
 * significaria nada, então o agrupamento carrega as duas pontas: o valor de face e o valor presente
 * estão na moeda do título, o liquido esta na moeda de pagamento.
 */
public record SettlementSummaryRow(
        String faceCurrency,
        String paymentCurrency,
        long operations,
        long receivables,
        BigDecimal totalFaceValue,
        BigDecimal totalPresentValue,
        BigDecimal totalNetAmount) {
}
