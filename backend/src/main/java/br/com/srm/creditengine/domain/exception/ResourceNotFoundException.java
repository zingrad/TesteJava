package br.com.srm.creditengine.domain.exception;

public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(String resource, Object identifier) {
        super("RESOURCE_NOT_FOUND", "Nenhum registro de %s para o identificador '%s'.".formatted(resource, identifier));
    }
}
