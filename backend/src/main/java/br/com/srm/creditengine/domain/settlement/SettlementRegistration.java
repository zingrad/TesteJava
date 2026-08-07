package br.com.srm.creditengine.domain.settlement;

import br.com.srm.creditengine.domain.pricing.PricingOrder;
import java.math.BigDecimal;
import java.util.List;

public record SettlementRegistration(
        String reference,
        String assignorTaxId,
        String faceCurrency,
        String paymentCurrency,
        BigDecimal baseMonthlyRate,
        List<PricingOrder.Receivable> items) {

    PricingOrder toPricingOrder() {
        return new PricingOrder(faceCurrency, paymentCurrency, baseMonthlyRate, items);
    }
}
