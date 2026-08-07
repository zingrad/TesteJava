package br.com.srm.creditengine.api.pricing;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Lote de recebíveis a precificar")
public record PricingSimulationRequest(

        @Schema(example = "BRL", description = "Moeda em que os títulos estão expressos")
        @NotNull
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um código ISO 4217 de três letras")
        String faceCurrency,

        @Schema(example = "USD", description = "Moeda em que o cedente será pago")
        @NotNull
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um código ISO 4217 de três letras")
        String paymentCurrency,

        @Schema(example = "0.01", description = "Taxa base mensal definida pela mesa, em fração decimal")
        @NotNull
        @DecimalMin(value = "0", message = "não pode ser negativa")
        @DecimalMax(value = "1", message = "excede o limite operacional de 100% ao mês")
        @Digits(integer = 1, fraction = 6, message = "aceita no máximo 6 casas decimais")
        BigDecimal baseMonthlyRate,

        @NotEmpty(message = "o lote precisa ter ao menos um título")
        @Size(max = 500, message = "o lote excede o limite de 500 títulos por operação")
        @Valid
        List<ReceivableRequest> items) {
}
