package com.switchtx.domain.model.user;

import com.switchtx.domain.model.shared.Guard;

import java.util.Collections;
import java.util.Set;

/**
 * Rol que agrupa permisos (ej. ADMIN, OPERATOR, AUDITOR, CUSTOMER).
 */
public record Role(Long id, String code, String name, String description, boolean active,
                   Set<Permission> permissions) {

    public Role {
        Guard.notBlank(code, "code", 30);
        Guard.notBlank(name, "name", 80);
        permissions = permissions != null ? Set.copyOf(permissions) : Collections.emptySet();
    }
}
