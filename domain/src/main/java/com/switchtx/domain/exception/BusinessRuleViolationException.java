package com.switchtx.domain.exception;

/**
 * Una regla de negocio impide completar la operación (se traduce a HTTP 422).
 * En operaciones monetarias este tipo de rechazo queda registrado como transacción REJECTED.
 */
public class BusinessRuleViolationException extends DomainException {

    public BusinessRuleViolationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessRuleViolationException(ErrorCode errorCode) {
        super(errorCode, null);
    }
}
