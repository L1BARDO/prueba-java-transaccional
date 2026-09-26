package com.switchtx.application.port.in.transaction;

import com.switchtx.domain.model.transaction.Transaction;

/**
 * Resultado de procesar una operación monetaria.
 *
 * @param replayed true si la respuesta corresponde a una operación ya procesada con la misma llave de idempotencia
 */
public record TransactionResult(Transaction transaction, boolean replayed) {

    public static TransactionResult created(Transaction transaction) {
        return new TransactionResult(transaction, false);
    }

    public static TransactionResult replayed(Transaction transaction) {
        return new TransactionResult(transaction, true);
    }
}
