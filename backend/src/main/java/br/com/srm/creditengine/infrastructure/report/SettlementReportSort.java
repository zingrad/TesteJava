package br.com.srm.creditengine.infrastructure.report;

/**
 * Colunas ordenáveis do extrato. O enum é a whitelist: o valor que chega na query string precisa
 * casar com uma constante, então não existe caminho por onde um nome de coluna arbitrário alcance o
 * SQL. Parametro inválido morre na conversão do Spring, antes do reader.
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
