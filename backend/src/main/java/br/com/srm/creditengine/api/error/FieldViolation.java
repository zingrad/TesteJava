package br.com.srm.creditengine.api.error;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Campo rejeitado na validação de entrada")
public record FieldViolation(

        @Schema(example = "items[0].faceValue")
        String field,

        @Schema(example = "deve ser maior que zero")
        String message) {
}
