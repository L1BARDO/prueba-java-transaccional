package com.switchtx.infrastructure.adapter.in.rest.transaction;

import com.switchtx.domain.model.transaction.Transaction;
import com.switchtx.domain.model.transaction.TransactionDetails;
import com.switchtx.domain.model.transaction.TransactionStatus;
import com.switchtx.domain.model.transaction.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Transacción procesada por el switch")
public record TransactionResponse(
        UUID id,
        @Schema(example = "TRX-20260926173045-9F3A1C7B") String reference,
        TransactionType type,
        TransactionStatus status,
        UUID sourceAccountId,
        UUID destinationAccountId,
        @Schema(example = "25000.00") BigDecimal amount,
        @Schema(example = "COP") String currency,
        String description,
        @Schema(description = "Código de rechazo (solo REJECTED)", example = "INSUFFICIENT_FUNDS") String failureCode,
        @Schema(description = "Motivo de rechazo (solo REJECTED)") String failureReason,
        Instant createdAt) {

    public static TransactionResponse from(Transaction t) {
        TransactionDetails d = t.getDetails();
        return new TransactionResponse(t.getId(), t.getReference(), d.type(), t.getStatus(), d.sourceAccountId(),
                d.destinationAccountId(), d.amount().amount(), d.amount().currency().getCurrencyCode(),
                d.description(), t.getFailureCode() != null ? t.getFailureCode().name() : null,
                t.getFailureReason(), t.getCreatedAt());
    }
}
