package com.switchtx.application.port.in.transaction;

import com.switchtx.domain.exception.InvalidDataException;
import com.switchtx.domain.model.transaction.TransactionStatus;
import com.switchtx.domain.model.transaction.TransactionType;

import java.time.Instant;
import java.util.UUID;

/**
 * Criterios opcionales de búsqueda de transacciones (null = sin filtro).
 * {@code accountId} busca la cuenta tanto como origen o como destino.
 */
public record TransactionFilter(UUID accountId, TransactionType type, TransactionStatus status,
                                Instant from, Instant to) {

    public TransactionFilter {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDataException("La fecha 'from' no puede ser posterior a 'to'");
        }
    }
}
