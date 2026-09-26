package com.switchtx.domain.model.transaction;

import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.shared.Guard;
import com.switchtx.domain.model.shared.Money;

import java.time.Instant;
import java.util.UUID;

/**
 * Asiento contable (ledger) que refleja el efecto de una transacción sobre una cuenta.
 * Una transferencia genera dos movimientos: DEBIT en origen y CREDIT en destino.
 */
public record Movement(UUID id,
                       UUID transactionId,
                       UUID accountId,
                       MovementType type,
                       Money amount,
                       Money balanceAfter,
                       Instant createdAt) {

    public Movement {
        Guard.notNull(id, "id");
        Guard.notNull(transactionId, "transactionId");
        Guard.notNull(accountId, "accountId");
        Guard.notNull(type, "type");
        Guard.notNull(amount, "amount");
        Guard.notNull(balanceAfter, "balanceAfter");
        Guard.notNull(createdAt, "createdAt");
    }

    /** Crea el movimiento a partir del estado de la cuenta ya afectada por la operación. */
    public static Movement record(Transaction transaction, Account account, MovementType type, Instant now) {
        return new Movement(UUID.randomUUID(), transaction.getId(), account.getId(), type,
                transaction.getDetails().amount(), account.getBalance(), now);
    }
}
