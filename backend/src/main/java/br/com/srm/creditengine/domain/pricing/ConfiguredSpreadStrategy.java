package br.com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;

/**
 * Base para os tipos cuja taxa e a soma da taxa base com o spread cadastrado. Manter o spread no
 * cadastro e nao no codigo permite a mesa ajustar risco sem deploy; a classe concreta existe para
 * amarrar o tipo a sua regra e abrir espaco para tipos que precisem de um calculo proprio.
 */
abstract class ConfiguredSpreadStrategy implements PricingStrategy {

    @Override
    public BigDecimal monthlyDiscountRate(PricingParameters parameters) {
        return parameters.baseMonthlyRate().add(parameters.receivableType().monthlySpread());
    }
}
