package com.switchtx.application.service;

import com.switchtx.application.port.in.transaction.DepositCommand;
import com.switchtx.application.port.in.transaction.DepositUseCase;
import com.switchtx.application.port.in.transaction.TransactionResult;
import com.switchtx.application.port.in.transaction.TransferCommand;
import com.switchtx.application.port.in.transaction.TransferUseCase;
import com.switchtx.application.port.in.transaction.WithdrawalCommand;
import com.switchtx.application.port.in.transaction.WithdrawalUseCase;
import com.switchtx.application.port.out.AccountRepositoryPort;
import com.switchtx.application.port.out.MovementRepositoryPort;
import com.switchtx.application.port.out.TransactionReferenceGenerator;
import com.switchtx.application.port.out.TransactionRepositoryPort;
import com.switchtx.application.port.out.UnitOfWork;
import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ConflictException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.shared.Money;
import com.switchtx.domain.model.transaction.Movement;
import com.switchtx.domain.model.transaction.MovementType;
import com.switchtx.domain.model.transaction.Transaction;
import com.switchtx.domain.model.transaction.TransactionDetails;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

/**
 * Motor del switch: aplica depósitos, retiros y transferencias de forma atómica.
 *
 * <p>Flujo de cada operación:
 * <ol>
 *   <li>Validación de la intención (monto, moneda, cuentas) antes de abrir transacción.</li>
 *   <li>Idempotencia: si la llave ya fue procesada se devuelve el resultado original.</li>
 *   <li>Transacción de BD con bloqueo pesimista de las cuentas involucradas.</li>
 *   <li>Si una regla de negocio rechaza la operación, se revierte todo y se registra la
 *       transacción como REJECTED en una transacción independiente (auditoría).</li>
 * </ol>
 */
@RequiredArgsConstructor
public class TransactionService implements DepositUseCase, WithdrawalUseCase, TransferUseCase {

    private static final Logger log = LogManager.getLogger(TransactionService.class);

    private final AccountRepositoryPort accounts;
    private final TransactionRepositoryPort transactions;
    private final MovementRepositoryPort movements;
    private final TransactionReferenceGenerator referenceGenerator;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    @Override
    public TransactionResult deposit(DepositCommand command) {
        TransactionDetails details = TransactionDetails.deposit(command.accountId(),
                Money.of(command.amount(), command.currency()), command.description(), command.idempotencyKey());

        return process(details, () -> {
            Instant now = clock.instant();
            Account account = lockAccount(details.destinationAccountId());
            account.credit(details.amount(), now);

            Transaction transaction = Transaction.completed(details, referenceGenerator.next(), now);
            persist(transaction, List.of(account),
                    List.of(Movement.record(transaction, account, MovementType.CREDIT, now)));
            return transaction;
        });
    }

    @Override
    public TransactionResult withdraw(WithdrawalCommand command) {
        TransactionDetails details = TransactionDetails.withdrawal(command.accountId(),
                Money.of(command.amount(), command.currency()), command.description(), command.idempotencyKey());

        return process(details, () -> {
            Instant now = clock.instant();
            Account account = lockAccount(details.sourceAccountId());
            account.debit(details.amount(), now);

            Transaction transaction = Transaction.completed(details, referenceGenerator.next(), now);
            persist(transaction, List.of(account),
                    List.of(Movement.record(transaction, account, MovementType.DEBIT, now)));
            return transaction;
        });
    }

    @Override
    public TransactionResult transfer(TransferCommand command) {
        TransactionDetails details = TransactionDetails.transfer(command.sourceAccountId(),
                command.destinationAccountId(), Money.of(command.amount(), command.currency()),
                command.description(), command.idempotencyKey());

        return process(details, () -> {
            Instant now = clock.instant();
            Map<UUID, Account> locked = lockAccounts(details.sourceAccountId(), details.destinationAccountId());
            Account source = locked.get(details.sourceAccountId());
            Account destination = locked.get(details.destinationAccountId());

            source.debit(details.amount(), now);
            destination.credit(details.amount(), now);

            Transaction transaction = Transaction.completed(details, referenceGenerator.next(), now);
            persist(transaction, List.of(source, destination), List.of(
                    Movement.record(transaction, source, MovementType.DEBIT, now),
                    Movement.record(transaction, destination, MovementType.CREDIT, now)));
            return transaction;
        });
    }

    private TransactionResult process(TransactionDetails details, Supplier<Transaction> operation) {
        Optional<Transaction> previous = findPreviousExecution(details);
        if (previous.isPresent()) {
            log.info("Reintento idempotente detectado key={} referencia={}", details.idempotencyKey(),
                    previous.get().getReference());
            return TransactionResult.replayed(previous.get());
        }

        try {
            Transaction transaction = unitOfWork.execute(operation);
            log.info("Transacción aplicada tipo={} referencia={} monto={}", transaction.getType(),
                    transaction.getReference(), details.amount());
            return TransactionResult.created(transaction);
        } catch (BusinessRuleViolationException ex) {
            // La transacción de BD ya se revirtió; se deja constancia del rechazo y se propaga el error original
            log.warn("Transacción rechazada tipo={} codigo={} motivo={}", details.type(), ex.getErrorCode(),
                    ex.getMessage());
            recordRejection(details, ex);
            throw ex;
        }
    }

    /**
     * Si la llave de idempotencia ya fue usada devuelve la transacción original. Una transacción previamente
     * rechazada vuelve a producir el mismo error, de modo que los reintentos obtienen siempre la misma respuesta.
     */
    private Optional<Transaction> findPreviousExecution(TransactionDetails details) {
        if (details.idempotencyKey() == null) {
            return Optional.empty();
        }
        Optional<Transaction> previous = unitOfWork.executeReadOnly(
                () -> transactions.findByIdempotencyKey(details.idempotencyKey()));

        previous.ifPresent(existing -> {
            if (!existing.getDetails().isSameOperationAs(details)) {
                throw new ConflictException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT,
                        "La llave de idempotencia '%s' ya fue usada con una operación diferente (referencia %s)"
                                .formatted(details.idempotencyKey(), existing.getReference()));
            }
            if (existing.isRejected()) {
                throw new BusinessRuleViolationException(existing.getFailureCode(), existing.getFailureReason());
            }
        });
        return previous;
    }

    /** Persiste el rechazo sin enmascarar el error de negocio original si la auditoría falla. */
    private void recordRejection(TransactionDetails details, BusinessRuleViolationException cause) {
        try {
            Transaction rejected = Transaction.rejected(details, referenceGenerator.next(), cause.getErrorCode(),
                    cause.getMessage(), clock.instant());
            unitOfWork.execute(() -> transactions.save(rejected));
            log.info("Rechazo registrado referencia={}", rejected.getReference());
        } catch (ConflictException ex) {
            log.warn("No se registró el rechazo: la llave de idempotencia '{}' ya fue usada por otra solicitud",
                    details.idempotencyKey());
        } catch (RuntimeException ex) {
            log.error("No fue posible registrar la transacción rechazada tipo={}", details.type(), ex);
        }
    }

    private void persist(Transaction transaction, List<Account> affected, List<Movement> ledgerEntries) {
        transactions.save(transaction);
        affected.forEach(accounts::save);
        movements.saveAll(ledgerEntries);
    }

    private Account lockAccount(UUID accountId) {
        return accounts.findByIdForUpdate(accountId).orElseThrow(() -> accountNotFound(accountId));
    }

    private Map<UUID, Account> lockAccounts(UUID... accountIds) {
        Set<UUID> ids = Set.of(accountIds);
        Map<UUID, Account> locked = accounts.findAllByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(Account::getId, Function.identity()));
        ids.stream()
                .filter(id -> !locked.containsKey(id))
                .findFirst()
                .ifPresent(missing -> {
                    throw accountNotFound(missing);
                });
        return locked;
    }

    private static ResourceNotFoundException accountNotFound(UUID accountId) {
        return new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                "No existe la cuenta %s".formatted(accountId));
    }
}
