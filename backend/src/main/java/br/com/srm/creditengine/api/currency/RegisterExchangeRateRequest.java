package br.com.srm.creditengine.api.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Cotacao informada manualmente pela mesa de operacao")
public record RegisterExchangeRateRequest(

        @Schema(example = "USD", description = "Moeda base, codigo ISO 4217")
        @NotNull
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um codigo ISO 4217 de tres letras")
        String baseCurrency,

        @Schema(example = "BRL", description = "Moeda de cotacao, codigo ISO 4217")
        @NotNull
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um codigo ISO 4217 de tres letras")
        String quoteCurrency,

        @Schema(example = "5.4321", description = "Quanto vale uma unidade da moeda base na moeda de cotacao")
        @NotNull
        @DecimalMin(value = "0", inclusive = false, message = "deve ser maior que zero")
        @DecimalMax(value = "1000000", message = "excede o limite operacional aceito")
        @Digits(integer = 10, fraction = 8, message = "aceita no maximo 8 casas decimais")
        BigDecimal rate,

        @Schema(description = "Momento em que a cotacao passa a vigorar; ausente significa agora")
        OffsetDateTime effectiveAt) {
}
