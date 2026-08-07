package br.com.srm.creditengine.api.pricing;

import br.com.srm.creditengine.domain.pricing.PricingOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Título que compõe o lote")
public record ReceivableRequest(

        @Schema(example = "DUP-2026-0001")
        @NotBlank
        @Size(max = 40)
        String documentNumber,

        @Schema(example = "MERCANTILE_INVOICE")
        @NotBlank
        @Size(max = 40)
        String receivableTypeCode,

        @Schema(example = "10000.00")
        @NotNull
        @DecimalMin(value = "0", inclusive = false, message = "deve ser maior que zero")
        @Digits(integer = 16, fraction = 2, message = "aceita no máximo 2 casas decimais")
        BigDecimal faceValue,

        @Schema(example = "2026-08-01")
        @NotNull
        LocalDate issueDate,

        @Schema(example = "2026-10-30")
        @NotNull
        LocalDate dueDate) {

    public PricingOrder.Receivable toDomain() {
        return new PricingOrder.Receivable(
                documentNumber.trim(), receivableTypeCode, faceValue, issueDate, dueDate);
    }
}
