package br.com.srm.creditengine.infrastructure.report;

import java.math.BigDecimal;

/**
 * Totais agrupados por par de moedas. Somar valores de moedas diferentes na mesma linha nao
 * significaria nada, entao o agrupamento carrega as duas pontas: o valor de face e o valor presente
 * estao na moeda do titulo, o liquido esta na moeda de pagamento.
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
