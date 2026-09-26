package com.switchtx.domain.exception;

/** La operación entra en conflicto con el estado actual del recurso (se traduce a HTTP 409). */
public class ConflictException extends DomainException {

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ConflictException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
