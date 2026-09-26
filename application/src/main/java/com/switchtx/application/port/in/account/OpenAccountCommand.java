package com.switchtx.application.port.in.account;

import com.switchtx.domain.model.account.AccountType;

import java.util.UUID;

public record OpenAccountCommand(UUID customerId, AccountType accountType, String currency) {
}
