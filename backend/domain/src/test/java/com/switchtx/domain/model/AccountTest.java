package com.switchtx.domain.model;

import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.account.AccountStatus;
import com.switchtx.domain.model.account.AccountType;
import com.switchtx.domain.model.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency COP = Currency.getInstance("COP");
    private final Instant now = Instant.parse("2026-09-26T12:00:00Z");
    private final UUID customerId = UUID.randomUUID();

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.open("1000000001", customerId, AccountType.SAVINGS, USD, now);
    }

    @Test
    @DisplayName("Abre cuenta en estado ACTIVE con saldo inicial cero")
    void shouldOpenAccountWithZeroBalance() {
        assertThat(account.getId()).isNotNull();
        assertThat(account.getAccountNumber()).isEqualTo("1000000001");
        assertThat(account.getCustomerId()).isEqualTo(customerId);
        assertThat(account.getType()).isEqualTo(AccountType.SAVINGS);
        assertThat(account.getCurrency()).isEqualTo(USD);
        assertThat(account.getBalance()).isEqualTo(Money.zero(USD));
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getCreatedAt()).isEqualTo(now);
        assertThat(account.getUpdatedAt()).isEqualTo(now);
        assertThat(account.isClosed()).isFalse();
    }

    @Test
    @DisplayName("Acredita saldo correctamente y actualiza updatedAt")
    void shouldCreditBalance() {
        Instant later = now.plusSeconds(60);
        Money amount = Money.of(new BigDecimal("250.00"), "USD");

        account.credit(amount, later);

        assertThat(account.getBalance()).isEqualTo(amount);
        assertThat(account.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    @DisplayName("Rechaza crédito si la cuenta está bloqueada")
    void shouldRejectCreditWhenAccountBlocked() {
        account.block(now);
        Money amount = Money.of(new BigDecimal("100.00"), "USD");

        assertThatThrownBy(() -> account.credit(amount, now))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    @Test
    @DisplayName("Rechaza crédito si la cuenta está cerrada")
    void shouldRejectCreditWhenAccountClosed() {
        account.close(now);
        Money amount = Money.of(new BigDecimal("100.00"), "USD");

        assertThatThrownBy(() -> account.credit(amount, now))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    @Test
    @DisplayName("Rechaza crédito si la moneda no coincide")
    void shouldRejectCreditWithCurrencyMismatch() {
        Money cop = Money.of(new BigDecimal("100000.00"), "COP");

        assertThatThrownBy(() -> account.credit(cop, now))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENCY_MISMATCH);
    }

    @Test
    @DisplayName("Debita saldo correctamente cuando hay fondos suficientes")
    void shouldDebitBalance() {
        account.credit(Money.of(new BigDecimal("500.00"), "USD"), now);
        Instant later = now.plusSeconds(30);

        account.debit(Money.of(new BigDecimal("200.00"), "USD"), later);

        assertThat(account.getBalance()).isEqualTo(Money.of(new BigDecimal("300.00"), "USD"));
        assertThat(account.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    @DisplayName("Rechaza débito cuando los fondos son insuficientes")
    void shouldRejectDebitWhenInsufficientFunds() {
        account.credit(Money.of(new BigDecimal("50.00"), "USD"), now);
        Money debitAmount = Money.of(new BigDecimal("100.00"), "USD");

        assertThatThrownBy(() -> account.debit(debitAmount, now))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
    }

    @Test
    @DisplayName("Rechaza débito si la cuenta está bloqueada")
    void shouldRejectDebitWhenBlocked() {
        account.credit(Money.of(new BigDecimal("500.00"), "USD"), now);
        account.block(now);

        assertThatThrownBy(() -> account.debit(Money.of(new BigDecimal("50.00"), "USD"), now))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    @Test
    @DisplayName("Rechaza débito si la moneda no coincide")
    void shouldRejectDebitWithCurrencyMismatch() {
        account.credit(Money.of(new BigDecimal("500.00"), "USD"), now);
        Money cop = Money.of(new BigDecimal("50000.00"), "COP");

        assertThatThrownBy(() -> account.debit(cop, now))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENCY_MISMATCH);
    }

    @Test
    @DisplayName("Transición ACTIVE -> BLOCKED -> ACTIVE permitida")
    void shouldBlockAndActivateAccount() {
        Instant t1 = now.plusSeconds(10);
        account.block(t1);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.BLOCKED);
        assertThat(account.getUpdatedAt()).isEqualTo(t1);

        Instant t2 = now.plusSeconds(20);
        account.activate(t2);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getUpdatedAt()).isEqualTo(t2);
    }

    @Test
    @DisplayName("Rechaza bloquear una cuenta que ya está bloqueada")
    void shouldRejectBlockWhenAlreadyBlocked() {
        account.block(now);

        assertThatThrownBy(() -> account.block(now))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("Rechaza activar una cuenta que ya está activa")
    void shouldRejectActivateWhenAlreadyActive() {
        assertThatThrownBy(() -> account.activate(now))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("Cierra la cuenta con saldo cero exitosamente")
    void shouldCloseAccountWithZeroBalance() {
        account.close(now);

        assertThat(account.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(account.isClosed()).isTrue();
    }

    @Test
    @DisplayName("Rechaza cerrar la cuenta si tiene saldo positivo")
    void shouldRejectCloseAccountWithPositiveBalance() {
        account.credit(Money.of(new BigDecimal("10.00"), "USD"), now);

        assertThatThrownBy(() -> account.close(now))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCOUNT_BALANCE_NOT_ZERO);
    }

    @Test
    @DisplayName("Rechaza cerrar una cuenta que ya está cerrada")
    void shouldRejectCloseWhenAlreadyClosed() {
        account.close(now);

        assertThatThrownBy(() -> account.close(now))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("Rechaza restaurar cuenta con moneda de balance diferente a la moneda de cuenta")
    void shouldRejectRestoreWithMismatchedCurrency() {
        UUID id = UUID.randomUUID();
        Money copBalance = Money.of(new BigDecimal("100.00"), "COP");

        assertThatThrownBy(() -> Account.restore(id, "1000000002", customerId, AccountType.CHECKING,
                USD, copBalance, AccountStatus.ACTIVE, now, now, 1L))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CURRENCY_MISMATCH);
    }
}
