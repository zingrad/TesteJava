package br.com.srm.creditengine.api.currency;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Cotação informada manualmente pela mesa de operação")
public record RegisterExchangeRateRequest(

        @Schema(example = "USD", description = "Moeda base, código ISO 4217")
        @NotNull
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um código ISO 4217 de três letras")
        String baseCurrency,

        @Schema(example = "BRL", description = "Moeda de cotação, código ISO 4217")
        @NotNull
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um código ISO 4217 de três letras")
        String quoteCurrency,

        @Schema(example = "5.4321", description = "Quanto vale uma unidade da moeda base na moeda de cotação")
        @NotNull
        @DecimalMin(value = "0", inclusive = false, message = "deve ser maior que zero")
        @DecimalMax(value = "1000000", message = "excede o limite operacional aceito")
        @Digits(integer = 10, fraction = 8, message = "aceita no máximo 8 casas decimais")
        BigDecimal rate,

        @Schema(description = "Momento em que a cotação passa a vigorar; ausente significa agora")
        OffsetDateTime effectiveAt) {
}
