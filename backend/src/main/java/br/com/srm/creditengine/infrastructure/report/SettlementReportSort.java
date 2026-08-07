package br.com.srm.creditengine.infrastructure.report;

/**
 * Colunas ordenaveis do extrato. O enum e a whitelist: o valor que chega na query string precisa
 * casar com uma constante, entao nao existe caminho por onde um nome de coluna arbitrario alcance o
 * SQL. Parametro invalido morre na conversao do Spring, antes do reader.
 */
public enum SettlementReportSort {

    SETTLED_AT("s.settled_at"),
    REQUESTED_AT("s.requested_at"),
    NET_AMOUNT("s.total_net_amount"),
    FACE_VALUE("s.total_face_value"),
    ASSIGNOR("a.legal_name");

    private final String column;

    SettlementReportSort(String column) {
        this.column = column;
    }

    String column() {
        return column;
    }
}
