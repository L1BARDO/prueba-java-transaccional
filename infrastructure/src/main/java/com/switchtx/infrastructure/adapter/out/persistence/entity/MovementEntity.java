package com.switchtx.infrastructure.adapter.out.persistence.entity;

import com.switchtx.domain.model.transaction.MovementType;
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
@Table(name = "account_movements")
public class MovementEntity extends InsertOnlyEntity {

    @Id
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 10)
    private MovementType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MovementEntity() {
        // requerido por JPA
    }

    public MovementEntity(UUID id, UUID transactionId, UUID accountId, MovementType type, BigDecimal amount,
                          BigDecimal balanceAfter, String currency, Instant createdAt) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.currency = currency;
        this.createdAt = createdAt;
    }

    @Override
    public UUID getId() { return id; }
    public UUID getTransactionId() { return transactionId; }
    public UUID getAccountId() { return accountId; }
    public MovementType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public String getCurrency() { return currency; }
    public Instant getCreatedAt() { return createdAt; }
}
