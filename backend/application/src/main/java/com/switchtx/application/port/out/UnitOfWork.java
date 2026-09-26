package com.switchtx.application.port.out;

import java.util.function.Supplier;

/**
 * Puerto de salida que delimita una transacción de base de datos sin acoplar
 * los casos de uso a un framework concreto. Si la operación lanza una excepción,
 * todos los cambios se revierten.
 */
public interface UnitOfWork {

    <T> T execute(Supplier<T> operation);

    <T> T executeReadOnly(Supplier<T> operation);

    default void run(Runnable operation) {
        execute(() -> {
            operation.run();
            return null;
        });
    }
}
