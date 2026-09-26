package com.switchtx.domain.model.shared;

import com.switchtx.domain.exception.InvalidDataException;

/** Validaciones reutilizables para proteger las invariantes del modelo. */
public final class Guard {

    private Guard() {
    }

    public static <T> T notNull(T value, String field) {
        if (value == null) {
            throw new InvalidDataException("El campo '%s' es obligatorio".formatted(field));
        }
        return value;
    }

    public static String notBlank(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException("El campo '%s' es obligatorio".formatted(field));
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidDataException("El campo '%s' supera %d caracteres".formatted(field, maxLength));
        }
        return trimmed;
    }

    public static String optional(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return notBlank(value, field, maxLength);
    }
}
