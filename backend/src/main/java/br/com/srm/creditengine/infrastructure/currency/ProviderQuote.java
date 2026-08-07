package br.com.srm.creditengine.infrastructure.currency;

import java.math.BigDecimal;

public record ProviderQuote(String baseCurrency, String quoteCurrency, BigDecimal rate) {
}
