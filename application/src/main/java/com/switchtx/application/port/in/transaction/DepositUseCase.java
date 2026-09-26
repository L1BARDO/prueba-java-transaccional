package com.switchtx.application.port.in.transaction;

public interface DepositUseCase {

    TransactionResult deposit(DepositCommand command);
}
