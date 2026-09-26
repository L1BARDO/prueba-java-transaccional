package com.switchtx.infrastructure.adapter.out.persistence.adapter;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.application.port.in.transaction.TransactionFilter;
import com.switchtx.application.port.out.TransactionRepositoryPort;
import com.switchtx.domain.exception.ConflictException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.model.transaction.Transaction;
import com.switchtx.infrastructure.adapter.out.persistence.entity.TransactionEntity;
import com.switchtx.infrastructure.adapter.out.persistence.mapper.ConstraintViolations;
import com.switchtx.infrastructure.adapter.out.persistence.mapper.PersistenceMapper;
import com.switchtx.infrastructure.adapter.out.persistence.repository.Specifications;
import com.switchtx.infrastructure.adapter.out.persistence.repository.TransactionJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository repository;

    public TransactionPersistenceAdapter(TransactionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Transaction save(Transaction transaction) {
        try {
            // flush inmediato: la violación de la llave de idempotencia se detecta aquí y no en el commit
            return PersistenceMapper.toDomain(repository.saveAndFlush(PersistenceMapper.toEntity(transaction)));
        } catch (DataIntegrityViolationException ex) {
            if (ConstraintViolations.isViolationOf(ex, TransactionEntity.UK_IDEMPOTENCY_KEY)) {
                throw new ConflictException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT,
                        "La llave de idempotencia '%s' está siendo procesada por otra solicitud"
                                .formatted(transaction.getDetails().idempotencyKey()), ex);
            }
            throw ex;
        }
    }

    @Override
    public Optional<Transaction> findById(UUID id) {
        return repository.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<Transaction> findByIdempotencyKey(String idempotencyKey) {
        return repository.findByIdempotencyKey(idempotencyKey).map(PersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Transaction> findAll(TransactionFilter filter, PageQuery pageQuery) {
        return PersistenceMapper.toPageResult(
                repository.findAll(Specifications.transactions(filter), PersistenceMapper.toPageable(pageQuery)),
                PersistenceMapper::toDomain);
    }
}
