package br.com.srm.creditengine.api.settlement;

import br.com.srm.creditengine.domain.settlement.Settlement;
import br.com.srm.creditengine.domain.settlement.SettlementItem;
import br.com.srm.creditengine.domain.settlement.SettlementStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "Operacao de cessao registrada")
public record SettlementResponse(

        String reference,
        String assignorTaxId,
        String assignorLegalName,
        String faceCurrency,
        String paymentCurrency,
        BigDecimal baseMonthlyRate,

        @Schema(description = "Cotacao travada na operacao; ausente quando nao e cross-currency")
        BigDecimal exchangeRate,

        BigDecimal totalFaceValue,
        BigDecimal totalPresentValue,
        BigDecimal totalNetAmount,
        SettlementStatus status,
        OffsetDateTime requestedAt,
        OffsetDateTime settledAt,
        List<Item> items) {

    static SettlementResponse from(Settlement settlement) {
        return new SettlementResponse(
                settlement.reference(),
                settlement.assignor().taxId(),
                settlement.assignor().legalName(),
                settlement.faceCurrency().code(),
                settlement.paymentCurrency().code(),
                settlement.baseMonthlyRate(),
                settlement.exchangeRateValue(),
                settlement.totalFaceValue(),
                settlement.totalPresentValue(),
                settlement.totalNetAmount(),
                settlement.status(),
                settlement.requestedAt(),
                settlement.settledAt(),
                settlement.items().stream().map(Item::from).toList());
    }

    @Schema(description = "Titulo do lote")
    record Item(
            String documentNumber,
            String receivableTypeCode,
            BigDecimal faceValue,
            LocalDate issueDate,
            LocalDate dueDate,
            int termDays,
            BigDecimal appliedSpread,
            BigDecimal presentValue,
            BigDecimal netAmount) {

        static Item from(SettlementItem item) {
            return new Item(
                    item.documentNumber(),
                    item.receivableType().code(),
                    item.faceValue(),
                    item.issueDate(),
                    item.dueDate(),
                    item.termDays(),
                    item.appliedSpread(),
                    item.presentValue(),
                    item.netAmount());
        }
    }
}
