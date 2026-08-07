package br.com.srm.creditengine.domain.exception;

/**
 * A requisição e valida, mas conflita com o estado atual do recurso. Distinta de
 * {@link BusinessRuleException} porque o cliente não corrige nada no payload: ou o estado muda,
 * ou a operação nunca vai passar.
 */
public class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
