-- =====================================================================================
--  01 - Creación de tablas, secuencias y restricciones
--  Se ejecuta con el usuario de la aplicación, conectado a la BD del switch.
--
--  Módulos:
--    NEGOCIO   : customers, accounts, transactions, account_movements
--    SEGURIDAD : users, roles, permissions, role_permissions, user_roles
--
--  Los índices están en 02_indices.sql; funciones y triggers en 03_funciones.sql.
-- =====================================================================================

-- Extensión para gen_random_uuid() y hash de contraseñas con bcrypt (crypt / gen_salt)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- #####################################################################################
--  MÓDULO NEGOCIO
-- #####################################################################################

-- -------------------------------------------------------------------------------------
--  CLIENTES
-- -------------------------------------------------------------------------------------
CREATE TABLE customers (
    id               UUID          NOT NULL DEFAULT gen_random_uuid(),
    document_type    VARCHAR(10)   NOT NULL,
    document_number  VARCHAR(20)   NOT NULL,
    full_name        VARCHAR(150)  NOT NULL,
    email            VARCHAR(150)  NOT NULL,
    phone            VARCHAR(20),
    status           VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version          BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uk_customers_document UNIQUE (document_type, document_number),
    CONSTRAINT uk_customers_email UNIQUE (email),
    CONSTRAINT ck_customers_document_type CHECK (document_type IN ('CC', 'CE', 'NIT', 'PASSPORT')),
    CONSTRAINT ck_customers_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT ck_customers_email_lower CHECK (email = lower(email))
);

COMMENT ON TABLE  customers         IS 'Titulares de cuentas. La baja es lógica (status = INACTIVE).';
COMMENT ON COLUMN customers.version IS 'Control de concurrencia optimista (JPA @Version).';

-- -------------------------------------------------------------------------------------
--  CUENTAS
-- -------------------------------------------------------------------------------------
CREATE SEQUENCE account_number_seq START WITH 1000000001 INCREMENT BY 1 NO CYCLE;

COMMENT ON SEQUENCE account_number_seq IS 'Generador de números de cuenta de 10 dígitos.';

CREATE TABLE accounts (
    id               UUID           NOT NULL DEFAULT gen_random_uuid(),
    account_number   VARCHAR(20)    NOT NULL,
    customer_id      UUID           NOT NULL,
    account_type     VARCHAR(10)    NOT NULL,
    currency         CHAR(3)        NOT NULL,
    balance          NUMERIC(19, 2) NOT NULL DEFAULT 0,
    status           VARCHAR(10)    NOT NULL DEFAULT 'ACTIVE',
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    version          BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT pk_accounts PRIMARY KEY (id),
    CONSTRAINT uk_accounts_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_accounts_type CHECK (account_type IN ('SAVINGS', 'CHECKING')),
    CONSTRAINT ck_accounts_status CHECK (status IN ('ACTIVE', 'BLOCKED', 'CLOSED')),
    CONSTRAINT ck_accounts_currency CHECK (currency ~ '^[A-Z]{3}$'),
    -- Última línea de defensa: el saldo nunca puede quedar negativo
    CONSTRAINT ck_accounts_balance_non_negative CHECK (balance >= 0),
    -- Una cuenta cerrada no puede tener saldo
    CONSTRAINT ck_accounts_closed_zero_balance CHECK (status <> 'CLOSED' OR balance = 0)
);

COMMENT ON TABLE  accounts         IS 'Cuentas de depósito. El saldo solo se modifica a través de transacciones.';
COMMENT ON COLUMN accounts.balance IS 'Saldo disponible con 2 decimales.';

-- -------------------------------------------------------------------------------------
--  TRANSACCIONES
-- -------------------------------------------------------------------------------------
CREATE TABLE transactions (
    id                      UUID           NOT NULL DEFAULT gen_random_uuid(),
    reference               VARCHAR(40)    NOT NULL,
    idempotency_key         VARCHAR(100),
    transaction_type        VARCHAR(15)    NOT NULL,
    status                  VARCHAR(15)    NOT NULL,
    source_account_id       UUID,
    destination_account_id  UUID,
    amount                  NUMERIC(19, 2) NOT NULL,
    currency                CHAR(3)        NOT NULL,
    description             VARCHAR(255),
    failure_code            VARCHAR(50),
    failure_reason          VARCHAR(255),
    created_at              TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT uk_transactions_reference UNIQUE (reference),
    CONSTRAINT uk_transactions_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT fk_transactions_source FOREIGN KEY (source_account_id) REFERENCES accounts (id),
    CONSTRAINT fk_transactions_destination FOREIGN KEY (destination_account_id) REFERENCES accounts (id),
    CONSTRAINT ck_transactions_type CHECK (transaction_type IN ('DEPOSIT', 'WITHDRAWAL', 'TRANSFER')),
    CONSTRAINT ck_transactions_status CHECK (status IN ('COMPLETED', 'REJECTED')),
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transactions_currency CHECK (currency ~ '^[A-Z]{3}$'),
    -- Cada tipo exige una combinación concreta de cuentas
    CONSTRAINT ck_transactions_accounts_by_type CHECK (
        (transaction_type = 'DEPOSIT'    AND source_account_id IS NULL     AND destination_account_id IS NOT NULL) OR
        (transaction_type = 'WITHDRAWAL' AND source_account_id IS NOT NULL AND destination_account_id IS NULL)     OR
        (transaction_type = 'TRANSFER'   AND source_account_id IS NOT NULL AND destination_account_id IS NOT NULL
                                         AND source_account_id <> destination_account_id)
    ),
    -- Una transacción rechazada siempre documenta el motivo
    CONSTRAINT ck_transactions_failure CHECK (
        (status = 'COMPLETED' AND failure_code IS NULL) OR
        (status = 'REJECTED'  AND failure_code IS NOT NULL)
    )
);

COMMENT ON TABLE  transactions                 IS 'Registro inmutable de todas las operaciones del switch (aplicadas y rechazadas).';
COMMENT ON COLUMN transactions.reference       IS 'Referencia única y legible para el cliente.';
COMMENT ON COLUMN transactions.idempotency_key IS 'Llave enviada por el cliente (header Idempotency-Key) para evitar dobles cobros.';

-- -------------------------------------------------------------------------------------
--  MOVIMIENTOS (LEDGER)
-- -------------------------------------------------------------------------------------
CREATE TABLE account_movements (
    id                UUID           NOT NULL DEFAULT gen_random_uuid(),
    transaction_id    UUID           NOT NULL,
    account_id        UUID           NOT NULL,
    movement_type     VARCHAR(10)    NOT NULL,
    amount            NUMERIC(19, 2) NOT NULL,
    balance_after     NUMERIC(19, 2) NOT NULL,
    currency          CHAR(3)        NOT NULL,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT pk_account_movements PRIMARY KEY (id),
    CONSTRAINT fk_movements_transaction FOREIGN KEY (transaction_id) REFERENCES transactions (id),
    CONSTRAINT fk_movements_account FOREIGN KEY (account_id) REFERENCES accounts (id),
    CONSTRAINT uk_movements_transaction_account UNIQUE (transaction_id, account_id),
    CONSTRAINT ck_movements_type CHECK (movement_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ck_movements_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_movements_balance_non_negative CHECK (balance_after >= 0)
);

COMMENT ON TABLE  account_movements               IS 'Libro contable: un asiento por cuenta afectada en cada transacción aplicada.';
COMMENT ON COLUMN account_movements.balance_after IS 'Saldo de la cuenta inmediatamente después del movimiento.';

-- #####################################################################################
--  MÓDULO SEGURIDAD (RBAC: usuarios → roles → permisos)
-- #####################################################################################

-- -------------------------------------------------------------------------------------
--  PERMISOS: acción atómica sobre un módulo (p. ej. TRANSACTION_TRANSFER)
-- -------------------------------------------------------------------------------------
CREATE TABLE permissions (
    id           BIGINT        GENERATED ALWAYS AS IDENTITY,
    code         VARCHAR(50)   NOT NULL,
    module       VARCHAR(30)   NOT NULL,
    description  VARCHAR(200)  NOT NULL,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_permissions PRIMARY KEY (id),
    CONSTRAINT uk_permissions_code UNIQUE (code),
    CONSTRAINT ck_permissions_code CHECK (code ~ '^[A-Z][A-Z_]*$'),
    CONSTRAINT ck_permissions_module CHECK (module IN ('CUSTOMER', 'ACCOUNT', 'TRANSACTION', 'SECURITY', 'REPORT'))
);

COMMENT ON TABLE permissions IS 'Catálogo de permisos del sistema.';

-- -------------------------------------------------------------------------------------
--  ROLES: agrupación de permisos
-- -------------------------------------------------------------------------------------
CREATE TABLE roles (
    id           BIGINT        GENERATED ALWAYS AS IDENTITY,
    code         VARCHAR(30)   NOT NULL,
    name         VARCHAR(80)   NOT NULL,
    description  VARCHAR(200),
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_code UNIQUE (code),
    CONSTRAINT ck_roles_code CHECK (code ~ '^[A-Z][A-Z_]*$')
);

COMMENT ON TABLE roles IS 'Roles de acceso. Un rol inactivo no otorga permisos.';

-- -------------------------------------------------------------------------------------
--  ROL ↔ PERMISO (N:M)
-- -------------------------------------------------------------------------------------
CREATE TABLE role_permissions (
    role_id        BIGINT       NOT NULL,
    permission_id  BIGINT       NOT NULL,
    granted_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_role_permissions PRIMARY KEY (role_id, permission_id),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_id) REFERENCES permissions (id) ON DELETE CASCADE
);

COMMENT ON TABLE role_permissions IS 'Permisos otorgados a cada rol.';

-- -------------------------------------------------------------------------------------
--  USUARIOS del sistema (operadores internos o clientes del portal)
-- -------------------------------------------------------------------------------------
CREATE TABLE users (
    id                     UUID          NOT NULL DEFAULT gen_random_uuid(),
    username               VARCHAR(50)   NOT NULL,
    email                  VARCHAR(150)  NOT NULL,
    password_hash          VARCHAR(100)  NOT NULL,
    full_name              VARCHAR(150)  NOT NULL,
    customer_id            UUID,
    status                 VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
    failed_login_attempts  INTEGER       NOT NULL DEFAULT 0,
    last_login_at          TIMESTAMPTZ,
    password_changed_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    version                BIGINT        NOT NULL DEFAULT 0,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_customer UNIQUE (customer_id),
    CONSTRAINT fk_users_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_users_username CHECK (username ~ '^[a-z0-9._-]{4,50}$'),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email)),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    CONSTRAINT ck_users_failed_attempts CHECK (failed_login_attempts >= 0),
    -- Solo se aceptan hashes bcrypt ($2a$, $2b$, $2y$), nunca contraseñas en texto plano
    CONSTRAINT ck_users_password_bcrypt CHECK (password_hash ~ '^\$2[aby]\$[0-9]{2}\$.{53}$')
);

COMMENT ON TABLE  users               IS 'Usuarios que acceden al sistema.';
COMMENT ON COLUMN users.password_hash IS 'Hash bcrypt de la contraseña.';
COMMENT ON COLUMN users.customer_id   IS 'Cliente asociado cuando el usuario es un titular (rol CUSTOMER).';

-- -------------------------------------------------------------------------------------
--  USUARIO ↔ ROL (N:M)
-- -------------------------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id      UUID         NOT NULL,
    role_id      BIGINT       NOT NULL,
    assigned_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    assigned_by  UUID,

    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id),
    CONSTRAINT fk_user_roles_assigned_by FOREIGN KEY (assigned_by) REFERENCES users (id) ON DELETE SET NULL
);

COMMENT ON TABLE user_roles IS 'Roles asignados a cada usuario.';
