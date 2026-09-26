package com.switchtx.domain.model.shared;

import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.InvalidDataException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value object inmutable que representa un valor monetario no negativo con su moneda ISO-4217.
 * Se trabaja siempre con 2 decimales para evitar errores de redondeo.
 */
public record Money(BigDecimal amount, Currency currency) {

    public static final int SCALE = 2;

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new InvalidDataException(ErrorCode.INVALID_AMOUNT, "El monto no puede ser negativo");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidDataException(ErrorCode.INVALID_AMOUNT,
                    "El monto admite máximo %d decimales".formatted(SCALE));
        }
        amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static Money of(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            throw new InvalidDataException(ErrorCode.INVALID_AMOUNT, "El monto es obligatorio");
        }
        return new Money(amount, currencyOf(currencyCode));
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    /** Traduce un código ISO-4217 a {@link Currency}, convirtiendo el error técnico en uno de dominio. */
    public static Currency currencyOf(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new InvalidDataException(ErrorCode.INVALID_CURRENCY, "La moneda es obligatoria");
        }
        try {
            return Currency.getInstance(currencyCode.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidDataException(ErrorCode.INVALID_CURRENCY,
                    "La moneda '%s' no es un código ISO-4217 válido".formatted(currencyCode), ex);
        }
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        if (isLessThan(other)) {
            throw new BusinessRuleViolationException(ErrorCode.INSUFFICIENT_FUNDS);
        }
        return new Money(amount.subtract(other.amount), currency);
    }

    public boolean isLessThan(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean hasSameCurrency(Money other) {
        return currency.equals(other.currency);
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!hasSameCurrency(other)) {
            throw new BusinessRuleViolationException(ErrorCode.CURRENCY_MISMATCH,
                    "No se pueden operar montos en %s y %s".formatted(currency, other.currency));
        }
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
