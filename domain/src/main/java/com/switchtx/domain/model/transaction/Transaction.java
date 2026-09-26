package com.switchtx.domain.model.transaction;

import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.model.shared.Guard;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro inmutable de una operación procesada por el switch (aplicada o rechazada).
 */
public final class Transaction {

    private final UUID id;
    private final String reference;
    private final TransactionDetails details;
    private final TransactionStatus status;
    private final ErrorCode failureCode;
    private final String failureReason;
    private final Instant createdAt;

    private Transaction(UUID id, String reference, TransactionDetails details, TransactionStatus status,
                        ErrorCode failureCode, String failureReason, Instant createdAt) {
        this.id = Guard.notNull(id, "id");
        this.reference = Guard.notBlank(reference, "reference", 40);
        this.details = Guard.notNull(details, "details");
        this.status = Guard.notNull(status, "status");
        this.createdAt = Guard.notNull(createdAt, "createdAt");
        if (status == TransactionStatus.REJECTED) {
            Guard.notNull(failureCode, "failureCode");
        }
        this.failureCode = failureCode;
        this.failureReason = Guard.optional(failureReason, "failureReason", 255);
    }

    public static Transaction completed(TransactionDetails details, String reference, Instant now) {
        return new Transaction(UUID.randomUUID(), reference, details, TransactionStatus.COMPLETED, null, null, now);
    }

    public static Transaction rejected(TransactionDetails details, String reference, ErrorCode failureCode,
                                       String failureReason, Instant now) {
        return new Transaction(UUID.randomUUID(), reference, details, TransactionStatus.REJECTED,
                failureCode, truncate(failureReason), now);
    }

    public static Transaction restore(UUID id, String reference, TransactionDetails details, TransactionStatus status,
                                      ErrorCode failureCode, String failureReason, Instant createdAt) {
        return new Transaction(id, reference, details, status, failureCode, failureReason, createdAt);
    }

    public boolean isRejected() {
        return status == TransactionStatus.REJECTED;
    }

    private static String truncate(String value) {
        return value != null && value.length() > 255 ? value.substring(0, 255) : value;
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public TransactionDetails getDetails() { return details; }
    public TransactionType getType() { return details.type(); }
    public TransactionStatus getStatus() { return status; }
    public ErrorCode getFailureCode() { return failureCode; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
