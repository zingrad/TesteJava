package br.com.srm.creditengine.api.pricing;

import br.com.srm.creditengine.domain.pricing.PricedReceivable;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Resultado da precificacao de um titulo")
public record PricedReceivableResponse(

        String documentNumber,
        String receivableTypeCode,
        BigDecimal faceValue,
        LocalDate issueDate,
        LocalDate dueDate,

        @Schema(description = "Dias corridos entre a data de referencia e o vencimento")
        int termDays,

        @Schema(description = "Spread de risco do tipo de recebivel, ao mes")
        BigDecimal appliedSpread,

        @Schema(description = "Taxa base somada ao spread, ao mes")
        BigDecimal monthlyDiscountRate,

        @Schema(description = "Valor presente na moeda do titulo")
        BigDecimal presentValue,

        @Schema(description = "Valor presente convertido para a moeda de pagamento")
        BigDecimal netAmount) {

    static PricedReceivableResponse from(PricedReceivable priced) {
        return new PricedReceivableResponse(
                priced.documentNumber(),
                priced.receivableTypeCode(),
                priced.faceValue(),
                priced.issueDate(),
                priced.dueDate(),
                priced.termDays(),
                priced.appliedSpread(),
                priced.monthlyDiscountRate(),
                priced.presentValue(),
                priced.netAmount());
    }
}
