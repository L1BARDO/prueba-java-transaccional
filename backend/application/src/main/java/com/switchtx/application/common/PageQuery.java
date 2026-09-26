package com.switchtx.application.common;

import com.switchtx.domain.exception.InvalidDataException;

/** Solicitud de paginación independiente de cualquier framework. */
public record PageQuery(int page, int size) {

    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new InvalidDataException("El número de página no puede ser negativo");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidDataException("El tamaño de página debe estar entre 1 y %d".formatted(MAX_SIZE));
        }
    }
}
