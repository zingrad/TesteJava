package br.com.srm.creditengine.api.currency;

import br.com.srm.creditengine.domain.currency.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange-rates")
@Validated
@Tag(name = "Câmbio")
class ExchangeRateController {

    private static final String ISO_CODE = "^[A-Za-z]{3}$";

    private final ExchangeRateService service;

    ExchangeRateController(ExchangeRateService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Registra uma cotação manualmente")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cotação registrada"),
            @ApiResponse(responseCode = "404", description = "Moeda desconhecida", content = @Content),
            @ApiResponse(responseCode = "409", description = "Já existe cotação do par nesse momento", content = @Content),
            @ApiResponse(responseCode = "422", description = "Par de moedas inválido", content = @Content)})
    ResponseEntity<ExchangeRateResponse> register(@Valid @RequestBody RegisterExchangeRateRequest request) {
        ExchangeRateResponse created = ExchangeRateResponse.from(service.register(
                request.baseCurrency(), request.quoteCurrency(), request.rate(), request.effectiveAt()));

        return ResponseEntity
                .created(URI.create("/api/exchange-rates/%d".formatted(created.id())))
                .body(created);
    }

    @PostMapping("/sync")
    @Operation(summary = "Importa as cotações vigentes do provedor externo",
            description = "A integração e simulada: gera uma variação em torno da referência USD/BRL.")
    List<ExchangeRateResponse> sync() {
        return service.syncFromProvider().stream().map(ExchangeRateResponse::from).toList();
    }

    @GetMapping("/current")
    @Operation(summary = "Cotação vigente de um par")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cotação vigente"),
            @ApiResponse(responseCode = "422", description = "Par sem cotação vigente", content = @Content)})
    ExchangeRateResponse current(
            @RequestParam @Pattern(regexp = ISO_CODE) String base,
            @RequestParam @Pattern(regexp = ISO_CODE) String quote) {

        return ExchangeRateResponse.from(service.currentRate(base, quote));
    }

    @GetMapping
    @Operation(summary = "Histórico de cotações de um par, da mais recente para a mais antiga")
    List<ExchangeRateResponse> history(
            @RequestParam @Pattern(regexp = ISO_CODE) String base,
            @RequestParam @Pattern(regexp = ISO_CODE) String quote,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit) {

        return service.history(base, quote, limit).stream().map(ExchangeRateResponse::from).toList();
    }
}
