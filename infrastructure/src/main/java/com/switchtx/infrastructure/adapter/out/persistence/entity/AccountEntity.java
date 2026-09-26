package com.switchtx.infrastructure.adapter.out.persistence.entity;

import com.switchtx.domain.model.account.AccountStatus;
import com.switchtx.domain.model.account.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Las relaciones se modelan por id (no @ManyToOne): cada agregado se carga de forma independiente. */
@Entity
@Table(name = "accounts")
public class AccountEntity {

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false, length = 20, updatable = false)
    private String accountNumber;

    @Column(name = "customer_id", nullable = false, updatable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 10, updatable = false)
    private AccountType accountType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected AccountEntity() {
        // requerido por JPA
    }

    public AccountEntity(UUID id, String accountNumber, UUID customerId, AccountType accountType, String currency,
                         BigDecimal balance, AccountStatus status, Instant createdAt, Instant updatedAt,
                         Long version) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.customerId = customerId;
        this.accountType = accountType;
        this.currency = currency;
        this.balance = balance;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public UUID getCustomerId() { return customerId; }
    public AccountType getAccountType() { return accountType; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public AccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
