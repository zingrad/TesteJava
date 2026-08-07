package br.com.srm.creditengine.api.settlement;

import br.com.srm.creditengine.domain.settlement.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settlements")
@Tag(name = "Liquidação")
class SettlementController {

    private final SettlementService settlements;

    SettlementController(SettlementService settlements) {
        this.settlements = settlements;
    }

    @PostMapping
    @Operation(summary = "Registra um lote para liquidação",
            description = "Precifica e grava o lote na mesma transação, com status PENDING. "
                    + "Reenviar a mesma referência não cria uma segunda operação.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lote registrado"),
            @ApiResponse(responseCode = "404", description = "Cedente, moeda ou tipo desconhecido", content = @Content),
            @ApiResponse(responseCode = "409", description = "Referência já registrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Lote recusado por regra de negócio", content = @Content)})
    ResponseEntity<SettlementResponse> register(@Valid @RequestBody RegisterSettlementRequest request) {
        SettlementResponse created = SettlementResponse.from(settlements.register(request.toDomain()));

        return ResponseEntity
                .created(URI.create("/api/settlements/%s".formatted(created.reference())))
                .body(created);
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Consulta uma operação pela referência")
    SettlementResponse find(@PathVariable String reference) {
        return SettlementResponse.from(settlements.findByReference(reference));
    }

    @PostMapping("/{reference}/settle")
    @Operation(summary = "Liquida a operação",
            description = "Transição única e protegida por lock otimista: duas chamadas simultaneas "
                    + "não liquidam a mesma operação duas vezes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação liquidada"),
            @ApiResponse(responseCode = "404", description = "Referência desconhecida", content = @Content),
            @ApiResponse(responseCode = "409", description = "Operação já liquidada, cancelada ou em disputa", content = @Content)})
    SettlementResponse settle(@PathVariable String reference) {
        return SettlementResponse.from(settlements.settle(reference));
    }

    @PostMapping("/{reference}/cancel")
    @Operation(summary = "Cancela uma operação ainda pendente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operação cancelada"),
            @ApiResponse(responseCode = "404", description = "Referência desconhecida", content = @Content),
            @ApiResponse(responseCode = "409", description = "Operação não esta mais pendente", content = @Content)})
    SettlementResponse cancel(@PathVariable String reference) {
        return SettlementResponse.from(settlements.cancel(reference));
    }
}
