package com.switchtx.application.service;

import com.switchtx.application.port.in.transaction.DepositCommand;
import com.switchtx.application.port.in.transaction.TransactionResult;
import com.switchtx.application.port.in.transaction.TransferCommand;
import com.switchtx.application.port.in.transaction.WithdrawalCommand;
import com.switchtx.application.port.out.AccountRepositoryPort;
import com.switchtx.application.port.out.MovementRepositoryPort;
import com.switchtx.application.port.out.TransactionReferenceGenerator;
import com.switchtx.application.port.out.TransactionRepositoryPort;
import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ConflictException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.account.AccountType;
import com.switchtx.domain.model.shared.Money;
import com.switchtx.domain.model.transaction.Movement;
import com.switchtx.domain.model.transaction.MovementType;
import com.switchtx.domain.model.transaction.Transaction;
import com.switchtx.domain.model.transaction.TransactionDetails;
import com.switchtx.domain.model.transaction.TransactionStatus;
import com.switchtx.domain.model.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepositoryPort accountRepository;

    @Mock
    private TransactionRepositoryPort transactionRepository;

    @Mock
    private MovementRepositoryPort movementRepository;

    @Mock
    private TransactionReferenceGenerator referenceGenerator;

    private final Instant now = Instant.parse("2026-09-26T12:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);
    private final Currency USD = Currency.getInstance("USD");

    private TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(accountRepository, transactionRepository, movementRepository,
                referenceGenerator, new FakeUnitOfWork(), clock);
    }

    @Test
    @DisplayName("Depósito exitoso: bloquea cuenta, suma saldo y guarda transacción COMPLETED")
    void shouldExecuteDepositSuccessfully() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.open("1000000001", UUID.randomUUID(), AccountType.SAVINGS, USD, now);
        given(accountRepository.findByIdForUpdate(accountId)).willReturn(Optional.of(account));
        given(referenceGenerator.next()).willReturn("TX-DEP-001");

        DepositCommand command = new DepositCommand(accountId, new BigDecimal("150.00"), "USD",
                "Depósito nómina", "key-dep-1");

        TransactionResult result = service.deposit(command);

        assertThat(result.replayed()).isFalse();
        assertThat(result.transaction().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(result.transaction().getReference()).isEqualTo("TX-DEP-001");
        assertThat(account.getBalance()).isEqualTo(Money.of(new BigDecimal("150.00"), "USD"));

        verify(transactionRepository).save(result.transaction());
        verify(accountRepository).save(account);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Movement>> captor = ArgumentCaptor.forClass(List.class);
        verify(movementRepository).saveAll(captor.capture());
        List<Movement> movements = captor.getValue();
        assertThat(movements).hasSize(1);
        assertThat(movements.getFirst().type()).isEqualTo(MovementType.CREDIT);
        assertThat(movements.getFirst().amount()).isEqualTo(Money.of(new BigDecimal("150.00"), "USD"));
    }

    @Test
    @DisplayName("Retiro exitoso: debita saldo y guarda transacción COMPLETED")
    void shouldExecuteWithdrawalSuccessfully() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.open("1000000001", UUID.randomUUID(), AccountType.SAVINGS, USD, now);
        account.credit(Money.of(new BigDecimal("300.00"), "USD"), now);

        given(accountRepository.findByIdForUpdate(accountId)).willReturn(Optional.of(account));
        given(referenceGenerator.next()).willReturn("TX-WTH-001");

        WithdrawalCommand command = new WithdrawalCommand(accountId, new BigDecimal("100.00"), "USD",
                "Retiro cajero", "key-wth-1");

        TransactionResult result = service.withdraw(command);

        assertThat(result.replayed()).isFalse();
        assertThat(result.transaction().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(account.getBalance()).isEqualTo(Money.of(new BigDecimal("200.00"), "USD"));

        verify(transactionRepository).save(result.transaction());
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("Retiro con fondos insuficientes: registra REJECTED en transacción separada y relanza excepción")
    void shouldRejectWithdrawalWhenInsufficientFundsAndRecordRejection() {
        UUID accountId = UUID.randomUUID();
        Account account = Account.open("1000000001", UUID.randomUUID(), AccountType.SAVINGS, USD, now);
        account.credit(Money.of(new BigDecimal("50.00"), "USD"), now);

        given(accountRepository.findByIdForUpdate(accountId)).willReturn(Optional.of(account));
        given(referenceGenerator.next()).willReturn("TX-REJ-001");

        WithdrawalCommand command = new WithdrawalCommand(accountId, new BigDecimal("100.00"), "USD",
                "Retiro cajero", "key-wth-rej");

        assertThatThrownBy(() -> service.withdraw(command))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);

        // Se verifica que la transacción de rechazo fue persistida
        ArgumentCaptor<Transaction> txCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        Transaction rejectedTx = txCaptor.getValue();

        assertThat(rejectedTx.getStatus()).isEqualTo(TransactionStatus.REJECTED);
        assertThat(rejectedTx.getFailureCode()).isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);
        assertThat(rejectedTx.getReference()).isEqualTo("TX-REJ-001");

        // El saldo de la cuenta no se alteró en la BD (la cuenta no se guardó tras el fallo)
        verify(accountRepository, never()).save(account);
        verify(movementRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Transferencia exitosa: debita origen, acredita destino y guarda 2 movimientos")
    void shouldExecuteTransferSuccessfully() {
        UUID sourceId = UUID.randomUUID();
        UUID destId = UUID.randomUUID();

        Account source = Account.restore(sourceId, "1000000001", UUID.randomUUID(), AccountType.SAVINGS,
                USD, Money.of(new BigDecimal("500.00"), "USD"), com.switchtx.domain.model.account.AccountStatus.ACTIVE,
                now, now, 1L);
        Account dest = Account.restore(destId, "1000000002", UUID.randomUUID(), AccountType.CHECKING,
                USD, Money.of(new BigDecimal("50.00"), "USD"), com.switchtx.domain.model.account.AccountStatus.ACTIVE,
                now, now, 1L);

        given(accountRepository.findAllByIdForUpdate(Set.of(sourceId, destId))).willReturn(List.of(source, dest));
        given(referenceGenerator.next()).willReturn("TX-TRF-001");

        TransferCommand command = new TransferCommand(sourceId, destId, new BigDecimal("150.00"), "USD",
                "Pago servicios", "key-trf-1");

        TransactionResult result = service.transfer(command);

        assertThat(result.replayed()).isFalse();
        assertThat(result.transaction().getStatus()).isEqualTo(TransactionStatus.COMPLETED);
        assertThat(source.getBalance()).isEqualTo(Money.of(new BigDecimal("350.00"), "USD"));
        assertThat(dest.getBalance()).isEqualTo(Money.of(new BigDecimal("200.00"), "USD"));

        verify(accountRepository).save(source);
        verify(accountRepository).save(dest);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Movement>> captor = ArgumentCaptor.forClass(List.class);
        verify(movementRepository).saveAll(captor.capture());
        List<Movement> movements = captor.getValue();
        assertThat(movements).hasSize(2);
        assertThat(movements.get(0).type()).isEqualTo(MovementType.DEBIT);
        assertThat(movements.get(1).type()).isEqualTo(MovementType.CREDIT);
    }

    @Test
    @DisplayName("Replay idempotente: si la transacción ya se completó devuelve el resultado previo")
    void shouldReplayCompletedTransaction() {
        UUID accountId = UUID.randomUUID();
        TransactionDetails details = TransactionDetails.deposit(accountId, Money.of(new BigDecimal("100.00"), "USD"),
                "Depósito", "key-idem-1");
        Transaction previous = Transaction.completed(details, "TX-PREV-001", now);

        given(transactionRepository.findByIdempotencyKey("key-idem-1")).willReturn(Optional.of(previous));

        DepositCommand command = new DepositCommand(accountId, new BigDecimal("100.00"), "USD",
                "Depósito", "key-idem-1");

        TransactionResult result = service.deposit(command);

        assertThat(result.replayed()).isTrue();
        assertThat(result.transaction()).isSameAs(previous);
        verify(accountRepository, never()).findByIdForUpdate(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Replay idempotente de transacción rechazada: relanza el mismo error de negocio")
    void shouldReplayRejectedTransactionAndThrowSameException() {
        UUID accountId = UUID.randomUUID();
        TransactionDetails details = TransactionDetails.withdrawal(accountId, Money.of(new BigDecimal("200.00"), "USD"),
                "Retiro", "key-idem-rej");
        Transaction rejected = Transaction.rejected(details, "TX-REJ-002", ErrorCode.INSUFFICIENT_FUNDS,
                "Fondos insuficientes", now);

        given(transactionRepository.findByIdempotencyKey("key-idem-rej")).willReturn(Optional.of(rejected));

        WithdrawalCommand command = new WithdrawalCommand(accountId, new BigDecimal("200.00"), "USD",
                "Retiro", "key-idem-rej");

        assertThatThrownBy(() -> service.withdraw(command))
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INSUFFICIENT_FUNDS);

        verify(accountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Conflicto de idempotencia: misma llave con datos diferentes lanza ConflictException")
    void shouldThrowConflictWhenKeyReusedForDifferentOperation() {
        UUID accountId = UUID.randomUUID();
        TransactionDetails details = TransactionDetails.deposit(accountId, Money.of(new BigDecimal("100.00"), "USD"),
                "Depósito 100", "key-conflict");
        Transaction previous = Transaction.completed(details, "TX-OLD-001", now);

        given(transactionRepository.findByIdempotencyKey("key-conflict")).willReturn(Optional.of(previous));

        // Intento con diferente monto (200.00 en vez de 100.00)
        DepositCommand command = new DepositCommand(accountId, new BigDecimal("200.00"), "USD",
                "Depósito 200", "key-conflict");

        assertThatThrownBy(() -> service.deposit(command))
                .isInstanceOf(ConflictException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);

        verify(accountRepository, never()).findByIdForUpdate(any());
    }
}
