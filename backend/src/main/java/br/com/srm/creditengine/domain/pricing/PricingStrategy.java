package br.com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;

/**
 * Regra de risco de um tipo de recebível. O motor de precificação conhece apenas a taxa mensal de
 * desconto que sai daqui; como cada tipo chega nela é problema da própria estratégia.
 */
public interface PricingStrategy {

    String receivableType();

    BigDecimal monthlyDiscountRate(PricingParameters parameters);
}
