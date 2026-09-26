package com.switchtx.domain.exception;

/**
 * Catálogo de códigos de error de negocio expuestos al cliente.
 * El código es estable y pensado para ser consumido por el frontend / integradores.
 */
public enum ErrorCode {

    // Validación de datos
    VALIDATION_ERROR("Los datos enviados no son válidos"),
    INVALID_REQUEST("La solicitud HTTP no es válida"),
    INVALID_AMOUNT("El monto no es válido"),
    INVALID_CURRENCY("La moneda no es válida"),

    // Recursos no encontrados
    CUSTOMER_NOT_FOUND("Cliente no encontrado"),
    ACCOUNT_NOT_FOUND("Cuenta no encontrada"),
    TRANSACTION_NOT_FOUND("Transacción no encontrada"),
    RESOURCE_NOT_FOUND("Recurso no encontrado"),

    // Conflictos
    CUSTOMER_ALREADY_EXISTS("El cliente ya existe"),
    IDEMPOTENCY_KEY_CONFLICT("La llave de idempotencia ya fue usada con otra operación"),
    CONCURRENCY_CONFLICT("El recurso fue modificado por otra operación"),
    DATA_INTEGRITY_VIOLATION("La operación viola una restricción de integridad"),

    // Reglas de negocio
    CUSTOMER_INACTIVE("El cliente no está activo"),
    CUSTOMER_HAS_OPEN_ACCOUNTS("El cliente tiene cuentas abiertas"),
    ACCOUNT_NOT_ACTIVE("La cuenta no está activa"),
    ACCOUNT_BALANCE_NOT_ZERO("La cuenta tiene saldo"),
    INVALID_STATUS_TRANSITION("Cambio de estado no permitido"),
    INSUFFICIENT_FUNDS("Fondos insuficientes"),
    CURRENCY_MISMATCH("La moneda no coincide con la de la cuenta"),
    SAME_ACCOUNT_TRANSFER("La cuenta origen y destino deben ser diferentes"),

    // Seguridad / Autenticación
    UNAUTHORIZED("No autenticado o token inválido"),
    FORBIDDEN("Acceso denegado: permisos insuficientes"),
    INVALID_CREDENTIALS("Usuario o contraseña incorrectos"),
    USER_LOCKED("El usuario se encuentra bloqueado"),
    USER_DISABLED("El usuario se encuentra deshabilitado"),
    USER_NOT_FOUND("Usuario no encontrado"),

    // Técnicos
    SERVICE_UNAVAILABLE("Servicio temporalmente no disponible"),
    INTERNAL_ERROR("Error interno del servidor");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
