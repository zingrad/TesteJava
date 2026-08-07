package br.com.srm.creditengine.domain.pricing;

import org.springframework.stereotype.Component;

/**
 * Cheque pre-datado: sem lastro em nota fiscal, carrega o spread mais alto do cadastro (2,5% a.m.).
 */
@Component
class PostDatedCheckStrategy extends ConfiguredSpreadStrategy {

    static final String CODE = "POST_DATED_CHECK";

    @Override
    public String receivableType() {
        return CODE;
    }
}
