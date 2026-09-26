package com.switchtx.domain.exception;

/** El recurso solicitado no existe (se traduce a HTTP 404). */
public class ResourceNotFoundException extends DomainException {

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
