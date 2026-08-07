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

@Schema(description = "Lote de recebiveis a precificar")
public record PricingSimulationRequest(

        @Schema(example = "BRL", description = "Moeda em que os titulos estao expressos")
        @NotNull
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um codigo ISO 4217 de tres letras")
        String faceCurrency,

        @Schema(example = "USD", description = "Moeda em que o cedente sera pago")
        @NotNull
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um codigo ISO 4217 de tres letras")
        String paymentCurrency,

        @Schema(example = "0.01", description = "Taxa base mensal definida pela mesa, em fracao decimal")
        @NotNull
        @DecimalMin(value = "0", message = "nao pode ser negativa")
        @DecimalMax(value = "1", message = "excede o limite operacional de 100% ao mes")
        @Digits(integer = 1, fraction = 6, message = "aceita no maximo 6 casas decimais")
        BigDecimal baseMonthlyRate,

        @NotEmpty(message = "o lote precisa ter ao menos um titulo")
        @Size(max = 500, message = "o lote excede o limite de 500 titulos por operacao")
        @Valid
        List<ReceivableRequest> items) {
}
