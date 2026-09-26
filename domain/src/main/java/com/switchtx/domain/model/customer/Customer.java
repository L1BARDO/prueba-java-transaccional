package com.switchtx.domain.model.customer;

import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.model.shared.Guard;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/** Titular de cuentas. La baja es lógica (estado INACTIVE) para conservar la trazabilidad. */
public final class Customer {

    private final UUID id;
    private final DocumentType documentType;
    private final String documentNumber;
    private String fullName;
    private String email;
    private String phone;
    private CustomerStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;

    private Customer(UUID id, DocumentType documentType, String documentNumber, String fullName, String email,
                     String phone, CustomerStatus status, Instant createdAt, Instant updatedAt, Long version) {
        this.id = Guard.notNull(id, "id");
        this.documentType = Guard.notNull(documentType, "documentType");
        this.documentNumber = Guard.notBlank(documentNumber, "documentNumber", 20);
        this.fullName = Guard.notBlank(fullName, "fullName", 150);
        this.email = normalizeEmail(email);
        this.phone = Guard.optional(phone, "phone", 20);
        this.status = Guard.notNull(status, "status");
        this.createdAt = Guard.notNull(createdAt, "createdAt");
        this.updatedAt = Guard.notNull(updatedAt, "updatedAt");
        this.version = version;
    }

    /** Registra un cliente nuevo en estado ACTIVE. */
    public static Customer register(DocumentType documentType, String documentNumber, String fullName,
                                    String email, String phone, Instant now) {
        return new Customer(UUID.randomUUID(), documentType, documentNumber, fullName, email, phone,
                CustomerStatus.ACTIVE, now, now, null);
    }

    /** Reconstruye un cliente existente (p. ej. desde la base de datos). */
    public static Customer restore(UUID id, DocumentType documentType, String documentNumber, String fullName,
                                   String email, String phone, CustomerStatus status, Instant createdAt,
                                   Instant updatedAt, Long version) {
        return new Customer(id, documentType, documentNumber, fullName, email, phone, status,
                createdAt, updatedAt, version);
    }

    public void updateContactInfo(String fullName, String email, String phone, Instant now) {
        ensureActive();
        this.fullName = Guard.notBlank(fullName, "fullName", 150);
        this.email = normalizeEmail(email);
        this.phone = Guard.optional(phone, "phone", 20);
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        if (status == CustomerStatus.INACTIVE) {
            throw new BusinessRuleViolationException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "El cliente %s ya se encuentra inactivo".formatted(id));
        }
        this.status = CustomerStatus.INACTIVE;
        this.updatedAt = now;
    }

    public void ensureActive() {
        if (status != CustomerStatus.ACTIVE) {
            throw new BusinessRuleViolationException(ErrorCode.CUSTOMER_INACTIVE,
                    "El cliente %s no está activo".formatted(id));
        }
    }

    private static String normalizeEmail(String email) {
        return Guard.notBlank(email, "email", 150).toLowerCase(Locale.ROOT);
    }

    public UUID getId() { return id; }
    public DocumentType getDocumentType() { return documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public CustomerStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
