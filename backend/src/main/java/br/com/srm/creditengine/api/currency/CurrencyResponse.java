package br.com.srm.creditengine.api.currency;

import br.com.srm.creditengine.domain.currency.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Moeda suportada pela plataforma")
public record CurrencyResponse(String code, String name, short minorUnit) {

    static CurrencyResponse from(Currency currency) {
        return new CurrencyResponse(currency.code(), currency.name(), currency.minorUnit());
    }
}
