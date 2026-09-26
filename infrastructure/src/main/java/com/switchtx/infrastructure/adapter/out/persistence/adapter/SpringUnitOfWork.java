package com.switchtx.infrastructure.adapter.out.persistence.adapter;

import com.switchtx.application.port.out.UnitOfWork;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Implementación del puerto {@link UnitOfWork} con transacciones programáticas de Spring.
 * Cualquier RuntimeException lanzada por la operación provoca rollback y se propaga tal cual.
 */
@Component
public class SpringUnitOfWork implements UnitOfWork {

    private final TransactionTemplate readWrite;
    private final TransactionTemplate readOnly;

    public SpringUnitOfWork(PlatformTransactionManager transactionManager) {
        this.readWrite = new TransactionTemplate(transactionManager);
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        return readWrite.execute(status -> operation.get());
    }

    @Override
    public <T> T executeReadOnly(Supplier<T> operation) {
        return readOnly.execute(status -> operation.get());
    }
}
