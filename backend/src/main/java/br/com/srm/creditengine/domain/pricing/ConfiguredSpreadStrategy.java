package br.com.srm.creditengine.domain.pricing;

import java.math.BigDecimal;

/**
 * Base para os tipos cuja taxa é a soma da taxa base com o spread cadastrado. Manter o spread no
 * cadastro e não no código permite a mesa ajustar risco sem deploy; a classe concreta existe para
 * amarrar o tipo a sua regra e abrir espaco para tipos que precisem de um cálculo próprio.
 */
abstract class ConfiguredSpreadStrategy implements PricingStrategy {

    @Override
    public BigDecimal monthlyDiscountRate(PricingParameters parameters) {
        return parameters.baseMonthlyRate().add(parameters.receivableType().monthlySpread());
    }
}
