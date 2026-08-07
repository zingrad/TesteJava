package br.com.srm.creditengine.api.currency;

import br.com.srm.creditengine.domain.currency.ExchangeRate;
import br.com.srm.creditengine.domain.currency.RateSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(description = "Cotacao registrada")
public record ExchangeRateResponse(
        Long id,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal rate,
        OffsetDateTime effectiveAt,
        RateSource source) {

    static ExchangeRateResponse from(ExchangeRate rate) {
        return new ExchangeRateResponse(
                rate.id(),
                rate.baseCurrency().code(),
                rate.quoteCurrency().code(),
                rate.rate(),
                rate.effectiveAt(),
                rate.source());
    }
}
