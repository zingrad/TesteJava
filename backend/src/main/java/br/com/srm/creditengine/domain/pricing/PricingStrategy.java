package br.com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;

/**
 * Regra de risco de um tipo de recebivel. O motor de precificacao conhece apenas a taxa mensal de
 * desconto que sai daqui; como cada tipo chega nela e problema da propria estrategia.
 */
public interface PricingStrategy {

    String receivableType();

    BigDecimal monthlyDiscountRate(PricingParameters parameters);
}
