package com.switchtx.application.port.in.transaction;

public interface WithdrawalUseCase {

    TransactionResult withdraw(WithdrawalCommand command);
}
