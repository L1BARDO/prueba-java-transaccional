package com.switchtx.domain.model.user;

import com.switchtx.domain.model.shared.Guard;

/**
 * Permiso atómico sobre un módulo del sistema (ej. TRANSACTION_TRANSFER).
 */
public record Permission(Long id, String code, String module, String description) {

    public Permission {
        Guard.notBlank(code, "code", 50);
        Guard.notBlank(module, "module", 30);
    }
}
