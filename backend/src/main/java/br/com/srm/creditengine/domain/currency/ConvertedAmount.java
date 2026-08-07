package br.com.srm.creditengine.domain.currency;

import java.math.BigDecimal;

/**
 * Resultado de uma conversão junto da cotação que a produziu. A liquidação guarda tanto o valor da
 * taxa quanto a referência da linha de origem, por isso ambos sobem para quem chama.
 */
public record ConvertedAmount(BigDecimal amount, BigDecimal rate, ExchangeRate appliedRate) {

    public boolean crossCurrency() {
        return appliedRate != null;
    }
}
