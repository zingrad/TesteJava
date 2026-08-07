package br.com.srm.creditengine.infrastructure.currency;

import java.util.List;

/**
 * Fonte externa de cotacoes. A implementacao atual e simulada; trocar por um cliente HTTP real
 * nao exige mudanca na camada de negocio.
 */
public interface ExchangeRateProvider {

    List<ProviderQuote> latestQuotes();
}
