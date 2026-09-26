package com.switchtx.application.service;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.application.port.in.transaction.TransactionFilter;
import com.switchtx.application.port.in.transaction.TransactionQueryUseCase;
import com.switchtx.application.port.out.TransactionRepositoryPort;
import com.switchtx.application.port.out.UnitOfWork;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.exception.ResourceNotFoundException;
import com.switchtx.domain.model.transaction.Transaction;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class TransactionQueryService implements TransactionQueryUseCase {

    private final TransactionRepositoryPort transactions;
    private final UnitOfWork unitOfWork;

    @Override
    public Transaction getById(UUID transactionId) {
        return unitOfWork.executeReadOnly(() -> transactions.findById(transactionId))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TRANSACTION_NOT_FOUND,
                        "No existe la transacción %s".formatted(transactionId)));
    }

    @Override
    public PageResult<Transaction> search(TransactionFilter filter, PageQuery pageQuery) {
        return unitOfWork.executeReadOnly(() -> transactions.findAll(filter, pageQuery));
    }
}
