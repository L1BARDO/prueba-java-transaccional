package com.switchtx.domain.model;

import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.InvalidDataException;
import com.switchtx.domain.model.shared.Money;
import com.switchtx.domain.model.transaction.TransactionDetails;
import com.switchtx.domain.model.transaction.TransactionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionDetailsTest {

    private final UUID account1 = UUID.randomUUID();
    private final UUID account2 = UUID.randomUUID();
    private final Money hundred = Money.of(new BigDecimal("100.00"), "USD");

    @Test
    @DisplayName("Crea detalles válidos para un depósito")
    void shouldCreateDepositDetails() {
        TransactionDetails details = TransactionDetails.deposit(account1, hundred, "Depósito inicial", "key-dep-1");

        assertThat(details.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(details.sourceAccountId()).isNull();
        assertThat(details.destinationAccountId()).isEqualTo(account1);
        assertThat(details.amount()).isEqualTo(hundred);
        assertThat(details.description()).isEqualTo("Depósito inicial");
        assertThat(details.idempotencyKey()).isEqualTo("key-dep-1");
    }

    @Test
    @DisplayName("Crea detalles válidos para un retiro")
    void shouldCreateWithdrawalDetails() {
        TransactionDetails details = TransactionDetails.withdrawal(account1, hundred, "Retiro cajero", "key-wth-1");

        assertThat(details.type()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(details.sourceAccountId()).isEqualTo(account1);
        assertThat(details.destinationAccountId()).isNull();
        assertThat(details.amount()).isEqualTo(hundred);
        assertThat(details.description()).isEqualTo("Retiro cajero");
        assertThat(details.idempotencyKey()).isEqualTo("key-wth-1");
    }

    @Test
    @DisplayName("Crea detalles válidos para una transferencia entre cuentas distintas")
    void shouldCreateTransferDetails() {
        TransactionDetails details = TransactionDetails.transfer(account1, account2, hundred, "Transferencia pago", "key-tx-1");

        assertThat(details.type()).isEqualTo(TransactionType.TRANSFER);
        assertThat(details.sourceAccountId()).isEqualTo(account1);
        assertThat(details.destinationAccountId()).isEqualTo(account2);
        assertThat(details.amount()).isEqualTo(hundred);
    }

    @Test
    @DisplayName("Rechaza transferencia a la misma cuenta con SAME_ACCOUNT_TRANSFER")
    void shouldRejectTransferToSameAccount() {
        assertThatThrownBy(() -> TransactionDetails.transfer(account1, account1, hundred, "Auto-transferencia", "key-same"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SAME_ACCOUNT_TRANSFER);
    }

    @Test
    @DisplayName("Rechaza monto cero o negativo")
    void shouldRejectZeroOrNegativeAmount() {
        Money zero = Money.zero(hundred.currency());

        assertThatThrownBy(() -> TransactionDetails.deposit(account1, zero, "Cero", "k1"))
                .isInstanceOf(InvalidDataException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_AMOUNT);
    }

    @Test
    @DisplayName("isSameOperationAs compara correctamente tipo, cuentas y monto")
    void shouldCheckIfSameOperation() {
        TransactionDetails base = TransactionDetails.transfer(account1, account2, hundred, "desc", "k1");
        TransactionDetails identical = TransactionDetails.transfer(account1, account2, hundred, "otra desc", "k2");
        TransactionDetails differentAmount = TransactionDetails.transfer(account1, account2,
                Money.of(new BigDecimal("200.00"), "USD"), "desc", "k1");
        TransactionDetails differentDest = TransactionDetails.transfer(account1, UUID.randomUUID(), hundred, "desc", "k1");
        TransactionDetails deposit = TransactionDetails.deposit(account2, hundred, "desc", "k1");

        assertThat(base.isSameOperationAs(identical)).isTrue();
        assertThat(base.isSameOperationAs(differentAmount)).isFalse();
        assertThat(base.isSameOperationAs(differentDest)).isFalse();
        assertThat(base.isSameOperationAs(deposit)).isFalse();
    }
}
