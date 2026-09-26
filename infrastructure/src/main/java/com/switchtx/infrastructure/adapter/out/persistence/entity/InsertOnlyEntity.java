package com.switchtx.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/**
 * Base para registros inmutables con id asignado por el dominio. Implementar {@link Persistable}
 * evita que Spring Data haga un SELECT previo (merge) al insertar: siempre se ejecuta un INSERT.
 */
@MappedSuperclass
public abstract class InsertOnlyEntity implements Persistable<UUID> {

    @Transient
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
