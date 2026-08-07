package br.com.srm.creditengine.infrastructure.currency;

import java.util.List;

/**
 * Fonte externa de cotações. A implementação atual e simulada; trocar por um cliente HTTP real
 * não exige mudança na camada de negócio.
 */
public interface ExchangeRateProvider {

    List<ProviderQuote> latestQuotes();
}
