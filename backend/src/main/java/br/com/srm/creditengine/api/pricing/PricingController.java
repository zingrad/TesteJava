package br.com.srm.creditengine.api.pricing;

import br.com.srm.creditengine.domain.pricing.PricingOrder;
import br.com.srm.creditengine.domain.pricing.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
@Tag(name = "Precificação")
class PricingController {

    private final PricingService pricing;

    PricingController(PricingService pricing) {
        this.pricing = pricing;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simula o deságio de um lote",
            description = "Calcula o valor presente de cada título e converte para a moeda de pagamento. "
                    + "Não registra nada: serve ao painel do operador antes de fechar a operação.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lote precificado"),
            @ApiResponse(responseCode = "404", description = "Moeda ou tipo de recebível desconhecido", content = @Content),
            @ApiResponse(responseCode = "422", description = "Lote recusado por regra de negócio", content = @Content)})
    PricingSimulationResponse simulate(@Valid @RequestBody PricingSimulationRequest request) {
        PricingOrder order = new PricingOrder(
                request.faceCurrency(),
                request.paymentCurrency(),
                request.baseMonthlyRate(),
                request.items().stream().map(ReceivableRequest::toDomain).toList());

        return PricingSimulationResponse.from(pricing.price(order));
    }
}
