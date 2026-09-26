package com.switchtx.application.port.out;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.application.port.in.transaction.TransactionFilter;
import com.switchtx.domain.model.transaction.Transaction;

import java.util.Optional;
import java.util.UUID;

/** Puerto de salida: persistencia de transacciones. */
public interface TransactionRepositoryPort {

    /**
     * Persiste la transacción.
     *
     * @throws com.switchtx.domain.exception.ConflictException si la llave de idempotencia ya existe
     */
    Transaction save(Transaction transaction);

    Optional<Transaction> findById(UUID id);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    PageResult<Transaction> findAll(TransactionFilter filter, PageQuery pageQuery);
}
