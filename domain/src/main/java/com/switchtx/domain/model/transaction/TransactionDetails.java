package com.switchtx.domain.model.transaction;

import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.InvalidDataException;
import com.switchtx.domain.model.shared.Guard;
import com.switchtx.domain.model.shared.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Intención de una operación monetaria, validada antes de tocar cualquier cuenta.
 * Garantiza la combinación correcta de cuentas según el {@link TransactionType}.
 */
public record TransactionDetails(TransactionType type,
                                 UUID sourceAccountId,
                                 UUID destinationAccountId,
                                 Money amount,
                                 String description,
                                 String idempotencyKey) {

    public TransactionDetails {
        Guard.notNull(type, "type");
        Guard.notNull(amount, "amount");
        if (!amount.isPositive()) {
            throw new InvalidDataException(ErrorCode.INVALID_AMOUNT, "El monto debe ser mayor a cero");
        }
        description = Guard.optional(description, "description", 255);
        idempotencyKey = Guard.optional(idempotencyKey, "idempotencyKey", 100);
    }

    public static TransactionDetails deposit(UUID accountId, Money amount, String description, String idempotencyKey) {
        return new TransactionDetails(TransactionType.DEPOSIT, null, Guard.notNull(accountId, "accountId"),
                amount, description, idempotencyKey);
    }

    public static TransactionDetails withdrawal(UUID accountId, Money amount, String description, String idempotencyKey) {
        return new TransactionDetails(TransactionType.WITHDRAWAL, Guard.notNull(accountId, "accountId"), null,
                amount, description, idempotencyKey);
    }

    public static TransactionDetails transfer(UUID sourceAccountId, UUID destinationAccountId, Money amount,
                                              String description, String idempotencyKey) {
        Guard.notNull(sourceAccountId, "sourceAccountId");
        Guard.notNull(destinationAccountId, "destinationAccountId");
        if (sourceAccountId.equals(destinationAccountId)) {
            throw new BusinessRuleViolationException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }
        return new TransactionDetails(TransactionType.TRANSFER, sourceAccountId, destinationAccountId,
                amount, description, idempotencyKey);
    }

    /** Indica si otra operación representa exactamente la misma intención (para validar idempotencia). */
    public boolean isSameOperationAs(TransactionDetails other) {
        return type == other.type
                && Objects.equals(sourceAccountId, other.sourceAccountId)
                && Objects.equals(destinationAccountId, other.destinationAccountId)
                && amount.equals(other.amount);
    }
}
