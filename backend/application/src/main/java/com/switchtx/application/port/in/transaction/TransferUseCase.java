package com.switchtx.application.port.in.transaction;

public interface TransferUseCase {

    TransactionResult transfer(TransferCommand command);
}
