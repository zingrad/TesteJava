package br.com.srm.creditengine.api.pricing;

import br.com.srm.creditengine.domain.pricing.ReceivableType;
import br.com.srm.creditengine.domain.pricing.ReceivableTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/receivable-types")
@Tag(name = "Precificacao")
class ReceivableTypeController {

    private final ReceivableTypeService receivableTypes;

    ReceivableTypeController(ReceivableTypeService receivableTypes) {
        this.receivableTypes = receivableTypes;
    }

    @GetMapping
    @Operation(summary = "Lista os tipos de recebivel disponiveis para operacao")
    List<ReceivableTypeResponse> list() {
        return receivableTypes.listActive().stream().map(ReceivableTypeResponse::from).toList();
    }

    @Schema(description = "Tipo de recebivel e seu spread de risco")
    record ReceivableTypeResponse(String code, String name, BigDecimal monthlySpread) {

        static ReceivableTypeResponse from(ReceivableType type) {
            return new ReceivableTypeResponse(type.code(), type.name(), type.monthlySpread());
        }
    }
}
