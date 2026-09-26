-- =====================================================================================
--  05 - Datos de prueba
--  Los saldos de las cuentas cuadran con su libro de movimientos:
--      SELECT * FROM fn_reconcile_balances();   -- debe devolver 0 filas
--
--  Se usan UUID fijos para poder referenciarlos fácilmente desde Postman / Swagger.
-- =====================================================================================

BEGIN;

-- #####################################################################################
--  SEGURIDAD
-- #####################################################################################

INSERT INTO permissions (code, module, description) VALUES
    ('CUSTOMER_READ',         'CUSTOMER',    'Consultar clientes'),
    ('CUSTOMER_CREATE',       'CUSTOMER',    'Registrar clientes'),
    ('CUSTOMER_UPDATE',       'CUSTOMER',    'Actualizar datos de clientes'),
    ('CUSTOMER_DELETE',       'CUSTOMER',    'Inactivar clientes'),
    ('ACCOUNT_READ',          'ACCOUNT',     'Consultar cuentas y extractos'),
    ('ACCOUNT_CREATE',        'ACCOUNT',     'Abrir cuentas'),
    ('ACCOUNT_UPDATE_STATUS', 'ACCOUNT',     'Bloquear / reactivar cuentas'),
    ('ACCOUNT_CLOSE',         'ACCOUNT',     'Cerrar cuentas'),
    ('TRANSACTION_READ',      'TRANSACTION', 'Consultar transacciones'),
    ('TRANSACTION_DEPOSIT',   'TRANSACTION', 'Realizar depósitos'),
    ('TRANSACTION_WITHDRAW',  'TRANSACTION', 'Realizar retiros'),
    ('TRANSACTION_TRANSFER',  'TRANSACTION', 'Realizar transferencias'),
    ('USER_READ',             'SECURITY',    'Consultar usuarios'),
    ('USER_MANAGE',           'SECURITY',    'Crear, editar y bloquear usuarios'),
    ('ROLE_MANAGE',           'SECURITY',    'Administrar roles y permisos'),
    ('REPORT_VIEW',           'REPORT',      'Ver reportes y conciliaciones');

INSERT INTO roles (code, name, description) VALUES
    ('ADMIN',    'Administrador', 'Acceso total al sistema'),
    ('OPERATOR', 'Operador',      'Opera clientes, cuentas y transacciones'),
    ('AUDITOR',  'Auditor',       'Solo lectura y reportes'),
    ('CUSTOMER', 'Cliente',       'Titular: consulta sus cuentas y transfiere');

-- ADMIN: todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

-- OPERATOR: gestión de negocio, sin seguridad
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM roles r JOIN permissions p ON p.module IN ('CUSTOMER', 'ACCOUNT', 'TRANSACTION')
 WHERE r.code = 'OPERATOR';

-- AUDITOR: todas las lecturas + reportes
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM roles r JOIN permissions p ON p.code LIKE '%\_READ' ESCAPE '\' OR p.code = 'REPORT_VIEW'
 WHERE r.code = 'AUDITOR';

-- CUSTOMER: consulta y transferencias propias
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
  FROM roles r JOIN permissions p ON p.code IN ('ACCOUNT_READ', 'TRANSACTION_READ', 'TRANSACTION_TRANSFER')
 WHERE r.code = 'CUSTOMER';

-- #####################################################################################
--  CLIENTES
-- #####################################################################################

INSERT INTO customers (id, document_type, document_number, full_name, email, phone, status, created_at, updated_at) VALUES
    ('a0000000-0000-4000-8000-000000000001', 'CC',       '1020304050', 'Ana María Pérez',    'ana.perez@mail.com',      '+573001112233', 'ACTIVE',   now() - interval '30 days', now() - interval '30 days'),
    ('a0000000-0000-4000-8000-000000000002', 'CC',       '80123456',   'Carlos Andrés Gómez', 'carlos.gomez@mail.com',   '+573104445566', 'ACTIVE',   now() - interval '29 days', now() - interval '29 days'),
    ('a0000000-0000-4000-8000-000000000003', 'NIT',      '900123456',  'Tech Solutions SAS', 'pagos@techsolutions.co',  '+576012345678', 'ACTIVE',   now() - interval '28 days', now() - interval '28 days'),
    ('a0000000-0000-4000-8000-000000000004', 'PASSPORT', 'AB1234567',  'John Smith',         'john.smith@mail.com',     NULL,            'INACTIVE', now() - interval '27 days', now() - interval '10 days');

-- #####################################################################################
--  CUENTAS (saldo final coherente con los movimientos de abajo)
--    1000000001  Ana     SAVINGS  COP  2.000.000,00
--    1000000002  Ana     CHECKING USD      1.500,00
--    1000000003  Carlos  SAVINGS  COP    600.000,00
--    1000000004  Tech    CHECKING COP  3.750.000,00
--    1000000005  Carlos  SAVINGS  COP    300.000,00  (BLOCKED)
-- #####################################################################################

INSERT INTO accounts (id, account_number, customer_id, account_type, currency, balance, status, created_at, updated_at) VALUES
    ('b0000000-0000-4000-8000-000000000001', nextval('account_number_seq'), 'a0000000-0000-4000-8000-000000000001', 'SAVINGS',  'COP', 2000000.00, 'ACTIVE',  now() - interval '30 days', now() - interval '2 days'),
    ('b0000000-0000-4000-8000-000000000002', nextval('account_number_seq'), 'a0000000-0000-4000-8000-000000000001', 'CHECKING', 'USD',    1500.00, 'ACTIVE',  now() - interval '30 days', now() - interval '20 days'),
    ('b0000000-0000-4000-8000-000000000003', nextval('account_number_seq'), 'a0000000-0000-4000-8000-000000000002', 'SAVINGS',  'COP',  600000.00, 'ACTIVE',  now() - interval '29 days', now() - interval '5 days'),
    ('b0000000-0000-4000-8000-000000000004', nextval('account_number_seq'), 'a0000000-0000-4000-8000-000000000003', 'CHECKING', 'COP', 3750000.00, 'ACTIVE',  now() - interval '28 days', now() - interval '3 days'),
    ('b0000000-0000-4000-8000-000000000005', nextval('account_number_seq'), 'a0000000-0000-4000-8000-000000000002', 'SAVINGS',  'COP',  300000.00, 'BLOCKED', now() - interval '29 days', now() - interval '1 days');

-- #####################################################################################
--  TRANSACCIONES
-- #####################################################################################

INSERT INTO transactions (id, reference, idempotency_key, transaction_type, status, source_account_id, destination_account_id, amount, currency, description, failure_code, failure_reason, created_at) VALUES
    ('d0000000-0000-4000-8000-000000000001', 'TRX-SEED-0001', 'seed-0001', 'DEPOSIT',    'COMPLETED', NULL,                                   'b0000000-0000-4000-8000-000000000001', 1000000.00, 'COP', 'Consignación inicial',        NULL, NULL, now() - interval '25 days'),
    ('d0000000-0000-4000-8000-000000000002', 'TRX-SEED-0002', 'seed-0002', 'DEPOSIT',    'COMPLETED', NULL,                                   'b0000000-0000-4000-8000-000000000003',  500000.00, 'COP', 'Consignación inicial',        NULL, NULL, now() - interval '24 days'),
    ('d0000000-0000-4000-8000-000000000003', 'TRX-SEED-0003', 'seed-0003', 'DEPOSIT',    'COMPLETED', NULL,                                   'b0000000-0000-4000-8000-000000000004', 5000000.00, 'COP', 'Aporte de capital',           NULL, NULL, now() - interval '23 days'),
    ('d0000000-0000-4000-8000-000000000004', 'TRX-SEED-0004', 'seed-0004', 'DEPOSIT',    'COMPLETED', NULL,                                   'b0000000-0000-4000-8000-000000000002',    1500.00, 'USD', 'Giro internacional',          NULL, NULL, now() - interval '20 days'),
    ('d0000000-0000-4000-8000-000000000005', 'TRX-SEED-0005', 'seed-0005', 'DEPOSIT',    'COMPLETED', NULL,                                   'b0000000-0000-4000-8000-000000000005',  300000.00, 'COP', 'Consignación',                NULL, NULL, now() - interval '15 days'),
    ('d0000000-0000-4000-8000-000000000006', 'TRX-SEED-0006', 'seed-0006', 'TRANSFER',   'COMPLETED', 'b0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000003',  200000.00, 'COP', 'Pago préstamo',               NULL, NULL, now() - interval '10 days'),
    ('d0000000-0000-4000-8000-000000000007', 'TRX-SEED-0007', 'seed-0007', 'WITHDRAWAL', 'COMPLETED', 'b0000000-0000-4000-8000-000000000003', NULL,                                    100000.00, 'COP', 'Retiro en cajero',            NULL, NULL, now() - interval '5 days'),
    ('d0000000-0000-4000-8000-000000000008', 'TRX-SEED-0008', 'seed-0008', 'TRANSFER',   'COMPLETED', 'b0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000001', 1250000.00, 'COP', 'Pago de nómina',              NULL, NULL, now() - interval '3 days'),
    ('d0000000-0000-4000-8000-000000000009', 'TRX-SEED-0009', 'seed-0009', 'WITHDRAWAL', 'COMPLETED', 'b0000000-0000-4000-8000-000000000001', NULL,                                     50000.00, 'COP', 'Retiro en oficina',           NULL, NULL, now() - interval '2 days'),
    -- Rechazadas: quedan registradas para auditoría pero no generan movimientos
    ('d0000000-0000-4000-8000-000000000010', 'TRX-SEED-0010', 'seed-0010', 'WITHDRAWAL', 'REJECTED',  'b0000000-0000-4000-8000-000000000003', NULL,                                   1000000.00, 'COP', 'Retiro en cajero',            'INSUFFICIENT_FUNDS', 'La cuenta 1000000003 no tiene fondos suficientes para debitar 1000000.00 COP', now() - interval '36 hours'),
    ('d0000000-0000-4000-8000-000000000011', 'TRX-SEED-0011', 'seed-0011', 'DEPOSIT',    'REJECTED',  NULL,                                   'b0000000-0000-4000-8000-000000000005',   10000.00, 'COP', 'Consignación',                'ACCOUNT_NOT_ACTIVE', 'La cuenta 1000000005 está en estado BLOCKED', now() - interval '12 hours');

-- #####################################################################################
--  MOVIMIENTOS (ledger) - solo transacciones COMPLETED
-- #####################################################################################

INSERT INTO account_movements (transaction_id, account_id, movement_type, amount, balance_after, currency, created_at) VALUES
    -- Depósitos
    ('d0000000-0000-4000-8000-000000000001', 'b0000000-0000-4000-8000-000000000001', 'CREDIT', 1000000.00, 1000000.00, 'COP', now() - interval '25 days'),
    ('d0000000-0000-4000-8000-000000000002', 'b0000000-0000-4000-8000-000000000003', 'CREDIT',  500000.00,  500000.00, 'COP', now() - interval '24 days'),
    ('d0000000-0000-4000-8000-000000000003', 'b0000000-0000-4000-8000-000000000004', 'CREDIT', 5000000.00, 5000000.00, 'COP', now() - interval '23 days'),
    ('d0000000-0000-4000-8000-000000000004', 'b0000000-0000-4000-8000-000000000002', 'CREDIT',    1500.00,    1500.00, 'USD', now() - interval '20 days'),
    ('d0000000-0000-4000-8000-000000000005', 'b0000000-0000-4000-8000-000000000005', 'CREDIT',  300000.00,  300000.00, 'COP', now() - interval '15 days'),
    -- Transferencia Ana → Carlos
    ('d0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000001', 'DEBIT',   200000.00,  800000.00, 'COP', now() - interval '10 days'),
    ('d0000000-0000-4000-8000-000000000006', 'b0000000-0000-4000-8000-000000000003', 'CREDIT',  200000.00,  700000.00, 'COP', now() - interval '10 days'),
    -- Retiro Carlos
    ('d0000000-0000-4000-8000-000000000007', 'b0000000-0000-4000-8000-000000000003', 'DEBIT',   100000.00,  600000.00, 'COP', now() - interval '5 days'),
    -- Transferencia Tech → Ana
    ('d0000000-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000004', 'DEBIT',  1250000.00, 3750000.00, 'COP', now() - interval '3 days'),
    ('d0000000-0000-4000-8000-000000000008', 'b0000000-0000-4000-8000-000000000001', 'CREDIT', 1250000.00, 2050000.00, 'COP', now() - interval '3 days'),
    -- Retiro Ana
    ('d0000000-0000-4000-8000-000000000009', 'b0000000-0000-4000-8000-000000000001', 'DEBIT',    50000.00, 2000000.00, 'COP', now() - interval '2 days');

-- #####################################################################################
--  USUARIOS (contraseñas con hash bcrypt generado por pgcrypto)
--    admin      / Admin123*     → ADMIN
--    operador   / Operador123*  → OPERATOR
--    auditor    / Auditor123*   → AUDITOR
--    ana.perez  / Cliente123*   → CUSTOMER (vinculado a la clienta Ana María Pérez)
--    bloqueado  / Bloqueado123* → OPERATOR (usuario LOCKED para pruebas)
-- #####################################################################################

INSERT INTO users (id, username, email, password_hash, full_name, customer_id, status, failed_login_attempts) VALUES
    ('e0000000-0000-4000-8000-000000000001', 'admin',     'admin@switchtx.co',     crypt('Admin123*',     gen_salt('bf', 10)), 'Administrador del sistema', NULL,                                   'ACTIVE', 0),
    ('e0000000-0000-4000-8000-000000000002', 'operador',  'operador@switchtx.co',  crypt('Operador123*',  gen_salt('bf', 10)), 'Operador de caja',          NULL,                                   'ACTIVE', 0),
    ('e0000000-0000-4000-8000-000000000003', 'auditor',   'auditor@switchtx.co',   crypt('Auditor123*',   gen_salt('bf', 10)), 'Auditor interno',           NULL,                                   'ACTIVE', 0),
    ('e0000000-0000-4000-8000-000000000004', 'ana.perez', 'ana.perez@mail.com',    crypt('Cliente123*',   gen_salt('bf', 10)), 'Ana María Pérez',           'a0000000-0000-4000-8000-000000000001', 'ACTIVE', 0),
    ('e0000000-0000-4000-8000-000000000005', 'bloqueado', 'bloqueado@switchtx.co', crypt('Bloqueado123*', gen_salt('bf', 10)), 'Usuario bloqueado',         NULL,                                   'LOCKED', 5);

INSERT INTO user_roles (user_id, role_id, assigned_by)
SELECT u.id, r.id, 'e0000000-0000-4000-8000-000000000001'
  FROM (VALUES ('admin', 'ADMIN'), ('operador', 'OPERATOR'), ('auditor', 'AUDITOR'),
               ('ana.perez', 'CUSTOMER'), ('bloqueado', 'OPERATOR')) AS x(username, role_code)
  JOIN users u ON u.username = x.username
  JOIN roles r ON r.code = x.role_code;

COMMIT;
