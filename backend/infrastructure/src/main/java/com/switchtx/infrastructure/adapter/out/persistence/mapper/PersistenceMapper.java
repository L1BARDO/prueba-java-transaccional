package com.switchtx.infrastructure.adapter.out.persistence.mapper;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.customer.Customer;
import com.switchtx.domain.model.shared.Money;
import com.switchtx.domain.model.transaction.Movement;
import com.switchtx.domain.model.transaction.Transaction;
import com.switchtx.domain.model.transaction.TransactionDetails;
import com.switchtx.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.switchtx.infrastructure.adapter.out.persistence.entity.CustomerEntity;
import com.switchtx.infrastructure.adapter.out.persistence.entity.MovementEntity;
import com.switchtx.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Currency;
import java.util.function.Function;

/** Traducción entre el modelo de dominio y las entidades JPA. El dominio nunca conoce JPA. */
public final class PersistenceMapper {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

    private PersistenceMapper() {
    }

    // ---- Customer ----

    public static CustomerEntity toEntity(Customer c) {
        return new CustomerEntity(c.getId(), c.getDocumentType(), c.getDocumentNumber(), c.getFullName(),
                c.getEmail(), c.getPhone(), c.getStatus(), c.getCreatedAt(), c.getUpdatedAt(), c.getVersion());
    }

    public static Customer toDomain(CustomerEntity e) {
        return Customer.restore(e.getId(), e.getDocumentType(), e.getDocumentNumber(), e.getFullName(),
                e.getEmail(), e.getPhone(), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt(), e.getVersion());
    }

    // ---- Account ----

    public static AccountEntity toEntity(Account a) {
        return new AccountEntity(a.getId(), a.getAccountNumber(), a.getCustomerId(), a.getType(),
                a.getCurrency().getCurrencyCode(), a.getBalance().amount(), a.getStatus(), a.getCreatedAt(),
                a.getUpdatedAt(), a.getVersion());
    }

    public static Account toDomain(AccountEntity e) {
        Currency currency = Currency.getInstance(e.getCurrency());
        return Account.restore(e.getId(), e.getAccountNumber(), e.getCustomerId(), e.getAccountType(), currency,
                new Money(e.getBalance(), currency), e.getStatus(), e.getCreatedAt(), e.getUpdatedAt(),
                e.getVersion());
    }

    // ---- Transaction ----

    public static TransactionEntity toEntity(Transaction t) {
        TransactionDetails d = t.getDetails();
        return new TransactionEntity(t.getId(), t.getReference(), d.idempotencyKey(), d.type(), t.getStatus(),
                d.sourceAccountId(), d.destinationAccountId(), d.amount().amount(),
                d.amount().currency().getCurrencyCode(), d.description(),
                t.getFailureCode() != null ? t.getFailureCode().name() : null, t.getFailureReason(),
                t.getCreatedAt());
    }

    public static Transaction toDomain(TransactionEntity e) {
        TransactionDetails details = new TransactionDetails(e.getType(), e.getSourceAccountId(),
                e.getDestinationAccountId(), Money.of(e.getAmount(), e.getCurrency()), e.getDescription(),
                e.getIdempotencyKey());
        return Transaction.restore(e.getId(), e.getReference(), details, e.getStatus(),
                toErrorCode(e.getFailureCode()), e.getFailureReason(), e.getCreatedAt());
    }

    // ---- Movement ----

    public static MovementEntity toEntity(Movement m) {
        return new MovementEntity(m.id(), m.transactionId(), m.accountId(), m.type(), m.amount().amount(),
                m.balanceAfter().amount(), m.amount().currency().getCurrencyCode(), m.createdAt());
    }

    public static Movement toDomain(MovementEntity e) {
        return new Movement(e.getId(), e.getTransactionId(), e.getAccountId(), e.getType(),
                Money.of(e.getAmount(), e.getCurrency()), Money.of(e.getBalanceAfter(), e.getCurrency()),
                e.getCreatedAt());
    }

    // ---- Paginación ----

    public static Pageable toPageable(PageQuery query) {
        return PageRequest.of(query.page(), query.size(), NEWEST_FIRST);
    }

    public static <E, D> PageResult<D> toPageResult(Page<E> page, Function<E, D> mapper) {
        return new PageResult<>(page.getContent().stream().map(mapper).toList(), page.getNumber(),
                page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    /** Tolera códigos históricos que ya no existan en el enum para no romper la lectura. */
    private static ErrorCode toErrorCode(String code) {
        if (code == null) {
            return null;
        }
        try {
            return ErrorCode.valueOf(code);
        } catch (IllegalArgumentException ex) {
            return ErrorCode.INTERNAL_ERROR;
        }
    }
}
