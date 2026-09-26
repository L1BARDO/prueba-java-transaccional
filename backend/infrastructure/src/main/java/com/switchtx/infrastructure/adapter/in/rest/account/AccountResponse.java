package com.switchtx.infrastructure.adapter.in.rest.account;

import com.switchtx.domain.model.account.Account;
import com.switchtx.domain.model.account.AccountStatus;
import com.switchtx.domain.model.account.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Cuenta")
public record AccountResponse(
        UUID id,
        @Schema(example = "1000000001") String accountNumber,
        UUID customerId,
        AccountType accountType,
        @Schema(example = "COP") String currency,
        @Schema(example = "150000.00") BigDecimal balance,
        AccountStatus status,
        Instant createdAt,
        Instant updatedAt,
        @Schema(example = "Ana María Pérez") String customerName,
        @Schema(example = "1020304050") String customerDocumentNumber) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getCustomerId(), a.getType(),
                a.getCurrency().getCurrencyCode(), a.getBalance().amount(), a.getStatus(), a.getCreatedAt(),
                a.getUpdatedAt(), null, null);
    }

    public static AccountResponse from(Account a, String customerName, String customerDocumentNumber) {
        return new AccountResponse(a.getId(), a.getAccountNumber(), a.getCustomerId(), a.getType(),
                a.getCurrency().getCurrencyCode(), a.getBalance().amount(), a.getStatus(), a.getCreatedAt(),
                a.getUpdatedAt(), customerName, customerDocumentNumber);
    }
}
