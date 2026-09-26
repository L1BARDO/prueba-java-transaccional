package com.switchtx.application.port.in.transaction;

import com.switchtx.application.common.PageQuery;
import com.switchtx.application.common.PageResult;
import com.switchtx.domain.model.transaction.Transaction;

import java.util.UUID;

public interface TransactionQueryUseCase {

    Transaction getById(UUID transactionId);

    PageResult<Transaction> search(TransactionFilter filter, PageQuery pageQuery);
}
