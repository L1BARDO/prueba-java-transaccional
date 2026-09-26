package com.switchtx.domain.model;

import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.InvalidDataException;
import com.switchtx.domain.model.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency COP = Currency.getInstance("COP");

    @Test
    @DisplayName("Crea Money válido con 2 decimales y escala normalizada")
    void shouldCreateMoneyWithTwoDecimals() {
        Money money = Money.of(new BigDecimal("150.5"), "USD");

        assertThat(money.amount()).isEqualByComparingTo("150.50");
        assertThat(money.currency()).isEqualTo(USD);
        assertThat(money.amount().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("Crea Money en cero para una moneda")
    void shouldCreateZeroMoney() {
        Money zero = Money.zero(USD);

        assertThat(zero.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(zero.isZero()).isTrue();
        assertThat(zero.isPositive()).isFalse();
        assertThat(zero.currency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("Rechaza montos negativos")
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-10.00"), "USD"))
                .isInstanceOf(InvalidDataException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @Test
    @DisplayName("Rechaza más de 2 decimales significativos")
    void shouldRejectMoreThanTwoDecimals() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("10.555"), "USD"))
                .isInstanceOf(InvalidDataException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @Test
    @DisplayName("Rechaza monto nulo")
    void shouldRejectNullAmount() {
        assertThatThrownBy(() -> Money.of(null, "USD"))
                .isInstanceOf(InvalidDataException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "INVALID", "123", "USDD"})
    @DisplayName("Rechaza monedas inválidas o en blanco")
    void shouldRejectInvalidCurrency(String currencyCode) {
        assertThatThrownBy(() -> Money.currencyOf(currencyCode))
                .isInstanceOf(InvalidDataException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CURRENCY);
    }

    @Test
    @DisplayName("Rechaza moneda nula")
    void shouldRejectNullCurrency() {
        assertThatThrownBy(() -> Money.currencyOf(null))
                .isInstanceOf(InvalidDataException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CURRENCY);
    }

    @Test
    @DisplayName("Suma correctamente montos de la misma moneda")
    void shouldAddSameCurrency() {
        Money m1 = Money.of(new BigDecimal("100.50"), "USD");
        Money m2 = Money.of(new BigDecimal("50.25"), "USD");

        Money result = m1.add(m2);

        assertThat(result.amount()).isEqualByComparingTo("150.75");
        assertThat(result.currency()).isEqualTo(USD);
    }

    @Test
    @DisplayName("Rechaza suma entre monedas distintas")
    void shouldRejectAddDifferentCurrencies() {
        Money usd = Money.of(new BigDecimal("100.00"), "USD");
        Money cop = Money.of(new BigDecimal("500000.00"), "COP");

        assertThatThrownBy(() -> usd.add(cop))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENCY_MISMATCH);
    }

    @Test
    @DisplayName("Resta correctamente montos con saldo suficiente")
    void shouldSubtractSameCurrency() {
        Money m1 = Money.of(new BigDecimal("100.00"), "USD");
        Money m2 = Money.of(new BigDecimal("40.00"), "USD");

        Money result = m1.subtract(m2);

        assertThat(result.amount()).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("Rechaza resta que daría saldo negativo con INSUFFICIENT_FUNDS")
    void shouldRejectSubtractWhenInsufficientFunds() {
        Money m1 = Money.of(new BigDecimal("30.00"), "USD");
        Money m2 = Money.of(new BigDecimal("50.00"), "USD");

        assertThatThrownBy(() -> m1.subtract(m2))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
    }

    @Test
    @DisplayName("Rechaza resta entre monedas distintas")
    void shouldRejectSubtractDifferentCurrencies() {
        Money usd = Money.of(new BigDecimal("100.00"), "USD");
        Money cop = Money.of(new BigDecimal("500000.00"), "COP");

        assertThatThrownBy(() -> usd.subtract(cop))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENCY_MISMATCH);
    }

    @Test
    @DisplayName("Compara montos y verifica predicados")
    void shouldCompareAndCheckPredicates() {
        Money low = Money.of(new BigDecimal("10.00"), "USD");
        Money high = Money.of(new BigDecimal("20.00"), "USD");
        Money zero = Money.zero(USD);

        assertThat(low.isLessThan(high)).isTrue();
        assertThat(high.isLessThan(low)).isFalse();
        assertThat(low.isPositive()).isTrue();
        assertThat(zero.isPositive()).isFalse();
        assertThat(zero.isZero()).isTrue();
        assertThat(low.hasSameCurrency(high)).isTrue();
        assertThat(low.toString()).isEqualTo("10.00 USD");
    }
}
