package br.com.srm.creditengine.api.currency;

import br.com.srm.creditengine.domain.currency.CurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/currencies")
@Tag(name = "Cambio")
class CurrencyController {

    private final CurrencyService currencies;

    CurrencyController(CurrencyService currencies) {
        this.currencies = currencies;
    }

    @GetMapping
    @Operation(summary = "Lista as moedas suportadas")
    List<CurrencyResponse> list() {
        return currencies.listSupported().stream().map(CurrencyResponse::from).toList();
    }
}
