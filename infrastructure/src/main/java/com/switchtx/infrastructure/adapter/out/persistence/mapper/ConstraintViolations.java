package com.switchtx.infrastructure.adapter.out.persistence.mapper;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

/** Utilidad para identificar qué restricción de BD provocó un {@link DataIntegrityViolationException}. */
public final class ConstraintViolations {

    private ConstraintViolations() {
    }

    public static Optional<String> constraintName(DataIntegrityViolationException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConstraintViolationException cve && cve.getConstraintName() != null) {
                return Optional.of(cve.getConstraintName());
            }
            current = current.getCause();
        }
        return Optional.empty();
    }

    public static boolean isViolationOf(DataIntegrityViolationException ex, String constraint) {
        return constraintName(ex).map(constraint::equalsIgnoreCase).orElse(false);
    }
}
