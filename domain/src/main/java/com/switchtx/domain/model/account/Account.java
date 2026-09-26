package com.switchtx.domain.model.account;

import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.model.shared.Guard;
import com.switchtx.domain.model.shared.Money;

import lombok.Getter;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

/**
 * Cuenta bancaria. Es la única dueña de su saldo: todo movimiento de dinero pasa por
 * {@link #credit(Money, Instant)} o {@link #debit(Money, Instant)}, que protegen las invariantes.
 */
@Getter
public final class Account {

    private final UUID id;
    private final String accountNumber;
    private final UUID customerId;
    private final AccountType type;
    private final Currency currency;
    private Money balance;
    private AccountStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;

    private Account(UUID id, String accountNumber, UUID customerId, AccountType type, Currency currency,
                    Money balance, AccountStatus status, Instant createdAt, Instant updatedAt, Long version) {
        this.id = Guard.notNull(id, "id");
        this.accountNumber = Guard.notBlank(accountNumber, "accountNumber", 20);
        this.customerId = Guard.notNull(customerId, "customerId");
        this.type = Guard.notNull(type, "type");
        this.currency = Guard.notNull(currency, "currency");
        this.balance = Guard.notNull(balance, "balance");
        this.status = Guard.notNull(status, "status");
        this.createdAt = Guard.notNull(createdAt, "createdAt");
        this.updatedAt = Guard.notNull(updatedAt, "updatedAt");
        this.version = version;
        if (!balance.currency().equals(currency)) {
            throw new BusinessRuleViolationException(ErrorCode.CURRENCY_MISMATCH,
                    "El saldo debe estar en la moneda de la cuenta");
        }
    }

    /** Apertura de una cuenta nueva: estado ACTIVE y saldo cero. */
    public static Account open(String accountNumber, UUID customerId, AccountType type, Currency currency,
                               Instant now) {
        return new Account(UUID.randomUUID(), accountNumber, customerId, type, currency, Money.zero(currency),
                AccountStatus.ACTIVE, now, now, null);
    }

    public static Account restore(UUID id, String accountNumber, UUID customerId, AccountType type,
                                  Currency currency, Money balance, AccountStatus status, Instant createdAt,
                                  Instant updatedAt, Long version) {
        return new Account(id, accountNumber, customerId, type, currency, balance, status, createdAt,
                updatedAt, version);
    }

    public void credit(Money amount, Instant now) {
        ensureCanTransact(amount);
        this.balance = balance.add(amount);
        this.updatedAt = now;
    }

    public void debit(Money amount, Instant now) {
        ensureCanTransact(amount);
        if (balance.isLessThan(amount)) {
            throw new BusinessRuleViolationException(ErrorCode.INSUFFICIENT_FUNDS,
                    "La cuenta %s no tiene fondos suficientes para debitar %s".formatted(accountNumber, amount));
        }
        this.balance = balance.subtract(amount);
        this.updatedAt = now;
    }

    public void block(Instant now) {
        if (status != AccountStatus.ACTIVE) {
            throw invalidTransition(AccountStatus.BLOCKED);
        }
        this.status = AccountStatus.BLOCKED;
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        if (status != AccountStatus.BLOCKED) {
            throw invalidTransition(AccountStatus.ACTIVE);
        }
        this.status = AccountStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void close(Instant now) {
        if (status == AccountStatus.CLOSED) {
            throw invalidTransition(AccountStatus.CLOSED);
        }
        if (!balance.isZero()) {
            throw new BusinessRuleViolationException(ErrorCode.ACCOUNT_BALANCE_NOT_ZERO,
                    "La cuenta %s debe tener saldo cero para cerrarse (saldo actual: %s)"
                            .formatted(accountNumber, balance));
        }
        this.status = AccountStatus.CLOSED;
        this.updatedAt = now;
    }

    public boolean isClosed() {
        return status == AccountStatus.CLOSED;
    }

    private void ensureCanTransact(Money amount) {
        Guard.notNull(amount, "amount");
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleViolationException(ErrorCode.ACCOUNT_NOT_ACTIVE,
                    "La cuenta %s está en estado %s".formatted(accountNumber, status));
        }
        if (!amount.currency().equals(currency)) {
            throw new BusinessRuleViolationException(ErrorCode.CURRENCY_MISMATCH,
                    "La cuenta %s opera en %s y la transacción está en %s"
                            .formatted(accountNumber, currency, amount.currency()));
        }
    }

    private BusinessRuleViolationException invalidTransition(AccountStatus target) {
        return new BusinessRuleViolationException(ErrorCode.INVALID_STATUS_TRANSITION,
                "La cuenta %s no puede pasar de %s a %s".formatted(accountNumber, status, target));
    }
}
