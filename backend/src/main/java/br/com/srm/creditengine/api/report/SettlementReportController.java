package br.com.srm.creditengine.api.report;

import br.com.srm.creditengine.infrastructure.report.SettlementReportFilter;
import br.com.srm.creditengine.infrastructure.report.SettlementReportFilter.SortDirection;
import br.com.srm.creditengine.infrastructure.report.SettlementReportReader;
import br.com.srm.creditengine.infrastructure.report.SettlementReportRow;
import br.com.srm.creditengine.infrastructure.report.SettlementReportSort;
import br.com.srm.creditengine.infrastructure.report.SettlementSummaryRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Relatórios são o caminho de duas camadas autorizado pelo item 6 do desafio: o controller fala
 * direto com o reader de SQL nativo, sem servico de negócio no meio, porque não ha regra a aplicar —
 * só um recorte de dados.
 */
@RestController
@RequestMapping("/api/reports/settlements")
@Validated
@Tag(name = "Relatórios")
class SettlementReportController {

    private final SettlementReportReader reader;

    SettlementReportController(SettlementReportReader reader) {
        this.reader = reader;
    }

    @GetMapping
    @Operation(summary = "Extrato de liquidação",
            description = "Recorte por período, cedente e moeda, paginado no servidor. As datas são "
                    + "inclusivas nas duas pontas, interpretadas em UTC e aplicadas sobre a data de "
                    + "liquidação — operações ainda pendentes não tem essa data e ficam fora de "
                    + "qualquer recorte por período.")
    PageResponse<SettlementReportRow> statement(
            @Parameter(description = "Início do período, inclusivo")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Fim do período, inclusivo")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @Parameter(description = "CNPJ do cedente, com ou sem pontuação")
            @RequestParam(required = false) String assignorTaxId,

            @RequestParam(required = false) @Pattern(regexp = "^[A-Za-z]{3}$",
                    message = "deve ser um código ISO 4217 de três letras") String paymentCurrency,

            @RequestParam(required = false) @Pattern(regexp = "^(?i)(PENDING|SETTLED|CANCELLED)$",
                    message = "deve ser PENDING, SETTLED ou CANCELLED") String status,

            @RequestParam(defaultValue = "SETTLED_AT") SettlementReportSort sort,
            @RequestParam(defaultValue = "DESC") SortDirection direction,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {

        return PageResponse.from(reader.statement(
                filter(from, to, assignorTaxId, paymentCurrency, status, sort, direction, page, size)));
    }

    @GetMapping("/summary")
    @Operation(summary = "Totais consolidados do extrato",
            description = "Mesmos filtros do extrato, agregados por par de moedas. O valor de face e o "
                    + "valor presente estão na moeda do título; o líquido, na moeda de pagamento.")
    List<SettlementSummaryRow> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String assignorTaxId,

            @RequestParam(required = false) @Pattern(regexp = "^[A-Za-z]{3}$",
                    message = "deve ser um código ISO 4217 de três letras") String paymentCurrency,

            @RequestParam(required = false) @Pattern(regexp = "^(?i)(PENDING|SETTLED|CANCELLED)$",
                    message = "deve ser PENDING, SETTLED ou CANCELLED") String status) {

        return reader.summary(filter(from, to, assignorTaxId, paymentCurrency, status,
                SettlementReportSort.SETTLED_AT, SortDirection.DESC, 0, 1));
    }

    private static SettlementReportFilter filter(LocalDate from, LocalDate to, String assignorTaxId,
            String paymentCurrency, String status, SettlementReportSort sort, SortDirection direction,
            int page, int size) {

        return new SettlementReportFilter(
                from, to, blankToNull(assignorTaxId), blankToNull(paymentCurrency), blankToNull(status),
                sort, direction, page, size);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
