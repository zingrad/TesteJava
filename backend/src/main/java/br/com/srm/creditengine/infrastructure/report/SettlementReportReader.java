package br.com.srm.creditengine.infrastructure.report;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Extrato analítico em SQL nativo. Não passa pela camada de negócio nem pelo ORM: o relatório le
 * colunas, não carrega agregados, e um lote de 500 títulos não precisa virar 500 objetos só para
 * somar três números. E o caminho de duas camadas que o item 6 do desafio autoriza para relatórios.
 */
@Repository
public class SettlementReportReader {

    private static final String STATEMENT_PROJECTION = """
            SELECT s.reference,
                   a.tax_id                AS assignor_tax_id,
                   a.legal_name            AS assignor_legal_name,
                   s.face_currency,
                   s.payment_currency,
                   s.status,
                   s.base_monthly_rate,
                   s.exchange_rate_value,
                   s.total_face_value,
                   s.total_present_value,
                   s.total_net_amount,
                   (SELECT COUNT(*) FROM settlement_item si WHERE si.settlement_id = s.id) AS item_count,
                   s.requested_at,
                   s.settled_at,
                   COUNT(*) OVER ()        AS total_elements
            """;

    private static final String SUMMARY_PROJECTION = """
            SELECT s.face_currency,
                   s.payment_currency,
                   COUNT(*)                            AS operations,
                   COALESCE(SUM(counted.item_count), 0) AS receivables,
                   SUM(s.total_face_value)             AS total_face_value,
                   SUM(s.total_present_value)          AS total_present_value,
                   SUM(s.total_net_amount)             AS total_net_amount
            """;

    private static final String FROM_CLAUSE = """
            FROM settlement s
              JOIN assignor a ON a.id = s.assignor_id
            """;

    private static final String SUMMARY_FROM_CLAUSE = """
            FROM settlement s
              JOIN assignor a ON a.id = s.assignor_id
              CROSS JOIN LATERAL (
                  SELECT COUNT(*) AS item_count FROM settlement_item si WHERE si.settlement_id = s.id
              ) counted
            """;

    private final NamedParameterJdbcTemplate jdbc;

    SettlementReportReader(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public PageResult<SettlementReportRow> statement(SettlementReportFilter filter) {
        List<TotalledRow> rows = jdbc.query(statementSql(filter), parameters(filter), STATEMENT_MAPPER);

        long totalElements = rows.isEmpty()
                ? countMatching(filter)
                : rows.getFirst().totalElements();

        return new PageResult<>(
                rows.stream().map(TotalledRow::row).toList(),
                totalElements,
                filter.page(),
                filter.size());
    }

    public List<SettlementSummaryRow> summary(SettlementReportFilter filter) {
        return jdbc.query(summarySql(filter), parameters(filter), SUMMARY_MAPPER);
    }

    /**
     * A janela {@code COUNT(*) OVER ()} traz o total junto das linhas, evitando uma segunda ida ao
     * banco no caso comum. So quando a página vem vazia — filtro sem resultado ou página além do fim —
     * e que o total precisa ser contado a parte.
     */
    private long countMatching(SettlementReportFilter filter) {
        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) " + FROM_CLAUSE + whereClause(filter), parameters(filter), Long.class);
        return total == null ? 0L : total;
    }

    static String statementSql(SettlementReportFilter filter) {
        return STATEMENT_PROJECTION + FROM_CLAUSE + whereClause(filter)
                + "ORDER BY %s %s NULLS LAST, s.id DESC\n"
                        .formatted(filter.sort().column(), filter.direction().name())
                + "LIMIT :size OFFSET :offset";
    }

    static String summarySql(SettlementReportFilter filter) {
        return SUMMARY_PROJECTION + SUMMARY_FROM_CLAUSE + whereClause(filter)
                + "GROUP BY s.face_currency, s.payment_currency\n"
                + "ORDER BY s.face_currency, s.payment_currency";
    }

    /**
     * Os predicados entram apenas quando o filtro correspondente veio preenchido. O padrão
     * {@code :param IS NULL OR coluna = :param} seria mais curto, mas cega o planejador: com o
     * predicado ausente do SQL, os índices compostos de {@code settled_at} continuam elegíveis.
     */
    static String whereClause(SettlementReportFilter filter) {
        List<String> predicates = new ArrayList<>();
        if (filter.status() != null) {
            predicates.add("s.status = :status");
        }
        if (filter.assignorTaxId() != null) {
            predicates.add("a.tax_id = :assignorTaxId");
        }
        if (filter.paymentCurrency() != null) {
            predicates.add("s.payment_currency = :paymentCurrency");
        }
        if (filter.from() != null) {
            predicates.add("s.settled_at >= :from");
        }
        if (filter.to() != null) {
            predicates.add("s.settled_at < :toExclusive");
        }
        return predicates.isEmpty() ? "" : "WHERE " + String.join("\n  AND ", predicates) + "\n";
    }

    static MapSqlParameterSource parameters(SettlementReportFilter filter) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("size", filter.size())
                .addValue("offset", filter.offset());

        if (filter.status() != null) {
            parameters.addValue("status", filter.status().trim().toUpperCase(Locale.ROOT));
        }
        if (filter.assignorTaxId() != null) {
            parameters.addValue("assignorTaxId", filter.assignorTaxId().replaceAll("\\D", ""));
        }
        if (filter.paymentCurrency() != null) {
            parameters.addValue("paymentCurrency", filter.paymentCurrency().trim().toUpperCase(Locale.ROOT));
        }
        if (filter.from() != null) {
            parameters.addValue("from", startOfDay(filter.from()));
        }
        if (filter.to() != null) {
            parameters.addValue("toExclusive", startOfDay(filter.to().plusDays(1)));
        }
        return parameters;
    }

    private static OffsetDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private record TotalledRow(SettlementReportRow row, long totalElements) {
    }

    private static final RowMapper<TotalledRow> STATEMENT_MAPPER = (ResultSet rs, int rowNum) ->
            new TotalledRow(new SettlementReportRow(
                    rs.getString("reference"),
                    rs.getString("assignor_tax_id"),
                    rs.getString("assignor_legal_name"),
                    rs.getString("face_currency"),
                    rs.getString("payment_currency"),
                    rs.getString("status"),
                    rs.getBigDecimal("base_monthly_rate"),
                    rs.getBigDecimal("exchange_rate_value"),
                    rs.getBigDecimal("total_face_value"),
                    rs.getBigDecimal("total_present_value"),
                    rs.getBigDecimal("total_net_amount"),
                    rs.getInt("item_count"),
                    offsetDateTime(rs, "requested_at"),
                    offsetDateTime(rs, "settled_at")),
                    rs.getLong("total_elements"));

    private static final RowMapper<SettlementSummaryRow> SUMMARY_MAPPER = (ResultSet rs, int rowNum) ->
            new SettlementSummaryRow(
                    rs.getString("face_currency"),
                    rs.getString("payment_currency"),
                    rs.getLong("operations"),
                    rs.getLong("receivables"),
                    zeroIfNull(rs.getBigDecimal("total_face_value")),
                    zeroIfNull(rs.getBigDecimal("total_present_value")),
                    zeroIfNull(rs.getBigDecimal("total_net_amount")));

    private static OffsetDateTime offsetDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
