package com.switchtx.application.port.out;

/** Puerto de salida: genera la referencia única y legible de una transacción. */
public interface TransactionReferenceGenerator {

    String next();
}
