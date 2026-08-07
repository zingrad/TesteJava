package br.com.srm.creditengine.domain.pricing;

import org.springframework.stereotype.Component;

/**
 * Duplicata mercantil: lastreada em nota fiscal, o risco fica no spread cadastrado (1,5% a.m.).
 */
@Component
class MercantileInvoiceStrategy extends ConfiguredSpreadStrategy {

    static final String CODE = "MERCANTILE_INVOICE";

    @Override
    public String receivableType() {
        return CODE;
    }
}
