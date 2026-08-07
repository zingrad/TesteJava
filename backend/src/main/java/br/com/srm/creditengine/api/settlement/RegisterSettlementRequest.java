package br.com.srm.creditengine.api.settlement;

import br.com.srm.creditengine.api.pricing.ReceivableRequest;
import br.com.srm.creditengine.domain.settlement.SettlementRegistration;
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

@Schema(description = "Lote a registrar para liquidacao")
public record RegisterSettlementRequest(

        @Schema(example = "OP-2026-0007",
                description = "Identificador da operacao no sistema de origem; reenvio nao duplica o registro")
        @NotNull
        @Pattern(regexp = "^[A-Za-z0-9._-]{4,40}$",
                message = "aceita de 4 a 40 caracteres entre letras, numeros, ponto, hifen e sublinhado")
        String reference,

        @Schema(example = "11222333000181", description = "CNPJ do cedente, com ou sem pontuacao")
        @NotNull
        @Pattern(regexp = "^\\D*(\\d\\D*){14}$", message = "deve conter 14 digitos")
        String assignorTaxId,

        @Schema(example = "BRL")
        @NotNull
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um codigo ISO 4217 de tres letras")
        String faceCurrency,

        @Schema(example = "USD")
        @NotNull
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "deve ser um codigo ISO 4217 de tres letras")
        String paymentCurrency,

        @Schema(example = "0.01")
        @NotNull
        @DecimalMin(value = "0", message = "nao pode ser negativa")
        @DecimalMax(value = "1", message = "excede o limite operacional de 100% ao mes")
        @Digits(integer = 1, fraction = 6, message = "aceita no maximo 6 casas decimais")
        BigDecimal baseMonthlyRate,

        @NotEmpty(message = "o lote precisa ter ao menos um titulo")
        @Size(max = 500, message = "o lote excede o limite de 500 titulos por operacao")
        @Valid
        List<ReceivableRequest> items) {

    SettlementRegistration toDomain() {
        return new SettlementRegistration(
                reference,
                assignorTaxId,
                faceCurrency,
                paymentCurrency,
                baseMonthlyRate,
                items.stream().map(ReceivableRequest::toDomain).toList());
    }
}
