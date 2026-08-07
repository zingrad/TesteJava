package br.com.srm.creditengine.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.srm.creditengine.infrastructure.report.SettlementReportFilter.SortDirection;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SettlementReportReaderTest {

    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 31);

    @Test
    void anUnfilteredStatementCarriesNoWhereClause() {
        assertThat(SettlementReportReader.whereClause(filter(null, null, null, null, null))).isEmpty();
        assertThat(SettlementReportReader.statementSql(filter(null, null, null, null, null)))
                .contains("LIMIT :size OFFSET :offset");
    }

    /**
     * O predicado ausente e o ponto: {@code :param IS NULL OR coluna = :param} manteria a coluna no
     * SQL e cegaria o planejador quanto aos indices compostos.
     */
    @Test
    void onlyTheSuppliedFiltersBecomePredicates() {
        String where = SettlementReportReader.whereClause(filter(FROM, TO, null, "USD", null));

        assertThat(where)
                .contains("s.settled_at >= :from")
                .contains("s.settled_at < :toExclusive")
                .contains("s.payment_currency = :paymentCurrency")
                .doesNotContain("a.tax_id")
                .doesNotContain("s.status");
    }

    @Test
    void everyFilterHasItsPredicateAndItsBoundValue() {
        SettlementReportFilter filter = filter(FROM, TO, "11.222.333/0001-81", "usd", "settled");

        assertThat(SettlementReportReader.statementSql(filter))
                .contains("s.status = :status")
                .contains("a.tax_id = :assignorTaxId")
                .contains("s.payment_currency = :paymentCurrency");

        var parameters = SettlementReportReader.parameters(filter);
        assertThat(parameters.getValue("status")).isEqualTo("SETTLED");
        assertThat(parameters.getValue("assignorTaxId")).isEqualTo("11222333000181");
        assertThat(parameters.getValue("paymentCurrency")).isEqualTo("USD");
    }

    /**
     * O fim do periodo e inclusivo para quem chama, mas vira um limite exclusivo no dia seguinte:
     * uma liquidacao as 23h59 do ultimo dia precisa entrar no recorte.
     */
    @Test
    void theClosingDateIsInclusiveForTheCallerAndExclusiveInTheQuery() {
        var parameters = SettlementReportReader.parameters(filter(FROM, TO, null, null, null));

        assertThat(parameters.getValue("from"))
                .isEqualTo(OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        assertThat(parameters.getValue("toExclusive"))
                .isEqualTo(OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC));
    }

    @Test
    void paginationTranslatesThePageNumberIntoAnOffset() {
        var parameters = SettlementReportReader.parameters(
                new SettlementReportFilter(null, null, null, null, null,
                        SettlementReportSort.SETTLED_AT, SortDirection.DESC, 3, 25));

        assertThat(parameters.getValue("size")).isEqualTo(25);
        assertThat(parameters.getValue("offset")).isEqualTo(75);
    }

    /**
     * A ordenacao e a unica parte do SQL montada por concatenacao, entao e onde a injecao entraria.
     * Como so um valor do enum chega ate aqui, o que vai para a query e sempre uma coluna conhecida.
     */
    @Test
    void everySortOptionResolvesToAKnownColumn() {
        for (SettlementReportSort sort : SettlementReportSort.values()) {
            String sql = SettlementReportReader.statementSql(
                    new SettlementReportFilter(null, null, null, null, null, sort, SortDirection.ASC, 0, 20));

            assertThat(sql).contains("ORDER BY " + sort.column() + " ASC NULLS LAST");
        }
    }

    @Test
    void theOrderIsBrokenByIdSoPagingCannotRepeatOrSkipRows() {
        String sql = SettlementReportReader.statementSql(filter(null, null, null, null, null));

        assertThat(sql).contains("ORDER BY s.settled_at DESC NULLS LAST, s.id DESC");
    }

    @Test
    void theSummaryGroupsByBothEndsOfTheCurrencyPair() {
        String sql = SettlementReportReader.summarySql(filter(FROM, TO, null, null, "SETTLED"));

        assertThat(sql)
                .contains("GROUP BY s.face_currency, s.payment_currency")
                .contains("s.status = :status")
                .doesNotContain("LIMIT");
    }

    private static SettlementReportFilter filter(LocalDate from, LocalDate to, String assignorTaxId,
            String paymentCurrency, String status) {

        return new SettlementReportFilter(from, to, assignorTaxId, paymentCurrency, status,
                SettlementReportSort.SETTLED_AT, SortDirection.DESC, 0, 20);
    }
}
