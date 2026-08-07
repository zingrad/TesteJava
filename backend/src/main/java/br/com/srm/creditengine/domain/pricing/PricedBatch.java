package br.com.srm.creditengine.domain.pricing;

import br.com.srm.creditengine.domain.currency.ExchangeRate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lote precificado. Carrega a cotação aplicada porque a liquidação precisa gravar tanto o valor da
 * taxa quanto a linha de origem, e o extrato precisa reproduzir o câmbio exato da operação.
 */
public record PricedBatch(
        LocalDate valuationDate,
        String faceCurrency,
        String paymentCurrency,
        BigDecimal baseMonthlyRate,
        BigDecimal exchangeRateValue,
        ExchangeRate appliedExchangeRate,
        BigDecimal totalFaceValue,
        BigDecimal totalPresentValue,
        BigDecimal totalNetAmount,
        List<PricedReceivable> items) {

    public boolean crossCurrency() {
        return appliedExchangeRate != null;
    }
}
