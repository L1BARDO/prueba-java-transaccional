package com.switchtx.domain.exception;

import java.util.Objects;

/**
 * Raíz de las excepciones del negocio. Siempre transporta un {@link ErrorCode}
 * para que los adaptadores de entrada puedan traducirla sin conocer el detalle.
 */
public abstract class DomainException extends RuntimeException {

    private final ErrorCode errorCode;

    protected DomainException(ErrorCode errorCode, String message) {
        super(message != null ? message : errorCode.defaultMessage());
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    protected DomainException(ErrorCode errorCode, String message, Throwable cause) {
        super(message != null ? message : errorCode.defaultMessage(), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
