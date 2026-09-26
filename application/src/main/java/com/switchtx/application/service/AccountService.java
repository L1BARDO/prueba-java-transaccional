package com.switchtx.application.service;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.application.port.in.account.AccountCommandUseCase;
import com.switchtx.application.port.in.account.AccountFilter;
import com.switchtx.application.port.in.account.AccountQueryUseCase;
import com.switchtx.application.port.in.account.OpenAccountCommand;
import com.switchtx.application.port.out.AccountRepositoryPort;
import com.switchtx.application.port.out.CustomerRepositoryPort;
import com.switchtx.application.port.out.MovementRepositoryPort;
import com.switchtx.application.port.out.UnitOfWork;
import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.account.AccountStatus;
import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.shared.Guard;
import com.switchtx.domain.model.shared.Money;
import com.switchtx.domain.model.transaction.Movement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.Clock;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

public class AccountService implements AccountCommandUseCase, AccountQueryUseCase {

    private static final Logger log = LogManager.getLogger(AccountService.class);

    private final AccountRepositoryPort accounts;
    private final CustomerRepositoryPort customers;
    private final MovementRepositoryPort movements;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public AccountService(AccountRepositoryPort accounts, CustomerRepositoryPort customers,
                          MovementRepositoryPort movements, UnitOfWork unitOfWork, Clock clock) {
        this.accounts = Objects.requireNonNull(accounts);
        this.customers = Objects.requireNonNull(customers);
        this.movements = Objects.requireNonNull(movements);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public Account open(OpenAccountCommand command) {
        Guard.notNull(command.customerId(), "customerId");
        Currency currency = Money.currencyOf(command.currency());

        Account opened = unitOfWork.execute(() -> {
            Customer customer = customers.findById(command.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND,
                            "No existe el cliente %s".formatted(command.customerId())));
            customer.ensureActive();

            Account account = Account.open(accounts.nextAccountNumber(), customer.getId(), command.accountType(),
                    currency, clock.instant());
            return accounts.save(account);
        });
        log.info("Cuenta abierta id={} numero={} cliente={} moneda={}", opened.getId(), opened.getAccountNumber(),
                opened.getCustomerId(), opened.getCurrency());
        return opened;
    }

    @Override
    public Account changeStatus(UUID accountId, AccountStatus targetStatus) {
        Guard.notNull(targetStatus, "status");
        Account updated = unitOfWork.execute(() -> {
            Account account = findOrThrow(accountId);
            switch (targetStatus) {
                case BLOCKED -> account.block(clock.instant());
                case ACTIVE -> account.activate(clock.instant());
                case CLOSED -> throw new BusinessRuleViolationException(ErrorCode.INVALID_STATUS_TRANSITION,
                        "Para cerrar una cuenta use la operación de cierre (DELETE)");
            }
            return accounts.save(account);
        });
        log.info("Cuenta id={} cambió a estado {}", accountId, targetStatus);
        return updated;
    }

    @Override
    public void close(UUID accountId) {
        unitOfWork.run(() -> {
            // Bloqueo pesimista: evita cerrar la cuenta mientras se aplica un movimiento concurrente
            Account account = accounts.findByIdForUpdate(accountId).orElseThrow(() -> notFound(accountId));
            account.close(clock.instant());
            accounts.save(account);
        });
        log.info("Cuenta cerrada id={}", accountId);
    }

    @Override
    public Account getById(UUID accountId) {
        return unitOfWork.executeReadOnly(() -> findOrThrow(accountId));
    }

    @Override
    public PageResult<Account> search(AccountFilter filter, PageQuery pageQuery) {
        return unitOfWork.executeReadOnly(() -> accounts.findAll(filter, pageQuery));
    }

    @Override
    public PageResult<Movement> getMovements(UUID accountId, PageQuery pageQuery) {
        return unitOfWork.executeReadOnly(() -> {
            findOrThrow(accountId);
            return movements.findByAccountId(accountId, pageQuery);
        });
    }

    private Account findOrThrow(UUID accountId) {
        return accounts.findById(accountId).orElseThrow(() -> notFound(accountId));
    }

    private static ResourceNotFoundException notFound(UUID accountId) {
        return new ResourceNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND,
                "No existe la cuenta %s".formatted(accountId));
    }
}
