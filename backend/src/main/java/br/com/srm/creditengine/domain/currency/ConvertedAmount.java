package br.com.srm.creditengine.domain.currency;

import java.math.BigDecimal;

/**
 * Resultado de uma conversao junto da cotacao que a produziu. A liquidacao guarda tanto o valor da
 * taxa quanto a referencia da linha de origem, por isso ambos sobem para quem chama.
 */
public record ConvertedAmount(BigDecimal amount, BigDecimal rate, ExchangeRate appliedRate) {

    public boolean crossCurrency() {
        return appliedRate != null;
    }
}
