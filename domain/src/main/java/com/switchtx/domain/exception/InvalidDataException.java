package com.switchtx.domain.exception;

/** Datos de entrada que violan una invariante del modelo (se traduce a HTTP 400). */
public class InvalidDataException extends DomainException {

    public InvalidDataException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public InvalidDataException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public InvalidDataException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
