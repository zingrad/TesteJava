package br.com.srm.creditengine.api.assignor;

import br.com.srm.creditengine.domain.assignor.Assignor;
import br.com.srm.creditengine.domain.assignor.AssignorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assignors")
@Tag(name = "Liquidacao")
class AssignorController {

    private final AssignorService assignors;

    AssignorController(AssignorService assignors) {
        this.assignors = assignors;
    }

    @GetMapping
    @Operation(summary = "Lista os cedentes cadastrados")
    List<AssignorResponse> list() {
        return assignors.listAll().stream().map(AssignorResponse::from).toList();
    }

    @Schema(description = "Cedente dos recebiveis")
    record AssignorResponse(Long id, String taxId, String legalName) {

        static AssignorResponse from(Assignor assignor) {
            return new AssignorResponse(assignor.id(), assignor.taxId(), assignor.legalName());
        }
    }
}
