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
@Tag(name = "Liquidacao")
class SettlementController {

    private final SettlementService settlements;

    SettlementController(SettlementService settlements) {
        this.settlements = settlements;
    }

    @PostMapping
    @Operation(summary = "Registra um lote para liquidacao",
            description = "Precifica e grava o lote na mesma transacao, com status PENDING. "
                    + "Reenviar a mesma referencia nao cria uma segunda operacao.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lote registrado"),
            @ApiResponse(responseCode = "404", description = "Cedente, moeda ou tipo desconhecido", content = @Content),
            @ApiResponse(responseCode = "409", description = "Referencia ja registrada", content = @Content),
            @ApiResponse(responseCode = "422", description = "Lote recusado por regra de negocio", content = @Content)})
    ResponseEntity<SettlementResponse> register(@Valid @RequestBody RegisterSettlementRequest request) {
        SettlementResponse created = SettlementResponse.from(settlements.register(request.toDomain()));

        return ResponseEntity
                .created(URI.create("/api/settlements/%s".formatted(created.reference())))
                .body(created);
    }

    @GetMapping("/{reference}")
    @Operation(summary = "Consulta uma operacao pela referencia")
    SettlementResponse find(@PathVariable String reference) {
        return SettlementResponse.from(settlements.findByReference(reference));
    }

    @PostMapping("/{reference}/settle")
    @Operation(summary = "Liquida a operacao",
            description = "Transicao unica e protegida por lock otimista: duas chamadas simultaneas "
                    + "nao liquidam a mesma operacao duas vezes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacao liquidada"),
            @ApiResponse(responseCode = "404", description = "Referencia desconhecida", content = @Content),
            @ApiResponse(responseCode = "409", description = "Operacao ja liquidada, cancelada ou em disputa", content = @Content)})
    SettlementResponse settle(@PathVariable String reference) {
        return SettlementResponse.from(settlements.settle(reference));
    }

    @PostMapping("/{reference}/cancel")
    @Operation(summary = "Cancela uma operacao ainda pendente")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Operacao cancelada"),
            @ApiResponse(responseCode = "404", description = "Referencia desconhecida", content = @Content),
            @ApiResponse(responseCode = "409", description = "Operacao nao esta mais pendente", content = @Content)})
    SettlementResponse cancel(@PathVariable String reference) {
        return SettlementResponse.from(settlements.cancel(reference));
    }
}
