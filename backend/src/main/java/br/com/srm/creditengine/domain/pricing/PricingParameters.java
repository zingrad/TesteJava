package br.com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;

/**
 * Tudo que uma estratégia pode considerar ao formar a taxa. Hoje as regras usam só a taxa base e o
 * spread do cadastro, mas o prazo entra no contrato porque risco de crédito e função do tempo.
 */
public record PricingParameters(
        BigDecimal baseMonthlyRate,
        ReceivableType receivableType,
        int termDays) {
}
