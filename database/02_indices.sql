-- =====================================================================================
--  02 - Índices
--  Las PK y UNIQUE ya generan su propio índice (definidas en 01_creates.sql).
--  Aquí se crean los índices de apoyo a FK y a las consultas más frecuentes.
-- =====================================================================================

-- ------------------------------------------------------------------- NEGOCIO

-- Cuentas de un cliente (GET /accounts?customerId=...) y validación al inactivar cliente
CREATE INDEX ix_accounts_customer_status ON accounts (customer_id, status);

-- Historial de transacciones por cuenta, como origen o como destino, ordenado por fecha
CREATE INDEX ix_transactions_source_created      ON transactions (source_account_id, created_at DESC)
    WHERE source_account_id IS NOT NULL;
CREATE INDEX ix_transactions_destination_created ON transactions (destination_account_id, created_at DESC)
    WHERE destination_account_id IS NOT NULL;

-- Listados generales y reportes por fecha
CREATE INDEX ix_transactions_created ON transactions (created_at DESC);

-- Reportes de rechazos (índice parcial: solo las filas REJECTED)
CREATE INDEX ix_transactions_rejected ON transactions (created_at DESC, failure_code)
    WHERE status = 'REJECTED';

-- Extracto de cuenta (GET /accounts/{id}/movements)
CREATE INDEX ix_movements_account_created ON account_movements (account_id, created_at DESC);

-- FK hacia transactions (join movimiento → transacción)
CREATE INDEX ix_movements_transaction ON account_movements (transaction_id);

-- Búsqueda de clientes por nombre sin distinguir mayúsculas
CREATE INDEX ix_customers_full_name_lower ON customers (lower(full_name));

-- ------------------------------------------------------------------- SEGURIDAD

CREATE INDEX ix_role_permissions_permission ON role_permissions (permission_id);
CREATE INDEX ix_user_roles_role             ON user_roles (role_id);
CREATE INDEX ix_users_status                ON users (status);
