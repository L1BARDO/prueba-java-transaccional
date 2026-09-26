package com.switchtx.domain.model.user;

import com.switchtx.domain.exception.BusinessRuleViolationException;
import com.switchtx.domain.exception.ErrorCode;
import com.switchtx.domain.model.shared.Guard;

import lombok.Getter;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Usuario del sistema con autenticación por contraseña cifrada y roles RBAC.
 */
@Getter
public final class User {

    private final UUID id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final String fullName;
    private final UUID customerId;
    private UserStatus status;
    private int failedLoginAttempts;
    private Instant lastLoginAt;
    private final Instant passwordChangedAt;
    private final Instant createdAt;
    private Instant updatedAt;
    private final Long version;
    private final Set<Role> roles;

    public User(UUID id, String username, String email, String passwordHash, String fullName,
                UUID customerId, UserStatus status, int failedLoginAttempts, Instant lastLoginAt,
                Instant passwordChangedAt, Instant createdAt, Instant updatedAt, Long version,
                Set<Role> roles) {
        this.id = Guard.notNull(id, "id");
        this.username = Guard.notBlank(username, "username", 50);
        this.email = Guard.notBlank(email, "email", 150);
        this.passwordHash = Guard.notBlank(passwordHash, "passwordHash", 100);
        this.fullName = Guard.notBlank(fullName, "fullName", 150);
        this.customerId = customerId;
        this.status = Guard.notNull(status, "status");
        this.failedLoginAttempts = Math.max(0, failedLoginAttempts);
        this.lastLoginAt = lastLoginAt;
        this.passwordChangedAt = Guard.notNull(passwordChangedAt, "passwordChangedAt");
        this.createdAt = Guard.notNull(createdAt, "createdAt");
        this.updatedAt = Guard.notNull(updatedAt, "updatedAt");
        this.version = version;
        this.roles = roles != null ? Set.copyOf(roles) : Collections.emptySet();
    }

    public void ensureCanAuthenticate() {
        if (status == UserStatus.LOCKED) {
            throw new BusinessRuleViolationException(ErrorCode.USER_LOCKED,
                    "El usuario %s se encuentra bloqueado por intentos fallidos".formatted(username));
        }
        if (status == UserStatus.DISABLED) {
            throw new BusinessRuleViolationException(ErrorCode.USER_DISABLED,
                    "El usuario %s se encuentra deshabilitado".formatted(username));
        }
    }

    public Set<String> getAllRoleCodes() {
        return roles.stream()
                .filter(Role::active)
                .map(Role::code)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<String> getAllPermissions() {
        return roles.stream()
                .filter(Role::active)
                .flatMap(r -> r.permissions().stream())
                .map(Permission::code)
                .collect(Collectors.toUnmodifiableSet());
    }
}
