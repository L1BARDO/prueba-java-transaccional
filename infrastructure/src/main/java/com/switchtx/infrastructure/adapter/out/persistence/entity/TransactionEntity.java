package com.switchtx.infrastructure.adapter.out.persistence.entity;

import com.switchtx.domain.model.transaction.TransactionStatus;
import com.switchtx.domain.model.transaction.TransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "transactions")
public class TransactionEntity extends InsertOnlyEntity {

    public static final String UK_IDEMPOTENCY_KEY = "uk_transactions_idempotency_key";

    @Id
    private UUID id;

    @Column(nullable = false, length = 40)
    private String reference;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 15)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TransactionStatus status;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(length = 255)
    private String description;

    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TransactionEntity() {
        // requerido por JPA
    }

    public TransactionEntity(UUID id, String reference, String idempotencyKey, TransactionType type,
                             TransactionStatus status, UUID sourceAccountId, UUID destinationAccountId,
                             BigDecimal amount, String currency, String description, String failureCode,
                             String failureReason, Instant createdAt) {
        this.id = id;
        this.reference = reference;
        this.idempotencyKey = idempotencyKey;
        this.type = type;
        this.status = status;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.failureCode = failureCode;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
    }

    @Override
    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public TransactionType getType() { return type; }
    public TransactionStatus getStatus() { return status; }
    public UUID getSourceAccountId() { return sourceAccountId; }
    public UUID getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public String getFailureCode() { return failureCode; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
