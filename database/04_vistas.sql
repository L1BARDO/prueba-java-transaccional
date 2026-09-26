-- =====================================================================================
--  04 - Vistas de consulta / reportes
-- =====================================================================================

-- Resumen de cuentas con su titular y actividad
CREATE OR REPLACE VIEW vw_account_summary AS
SELECT a.id                 AS account_id,
       a.account_number,
       a.account_type,
       a.currency,
       a.balance,
       a.status             AS account_status,
       c.id                 AS customer_id,
       c.document_type,
       c.document_number,
       c.full_name          AS customer_name,
       COUNT(m.id)          AS movements_count,
       MAX(m.created_at)    AS last_movement_at
  FROM accounts a
  JOIN customers c              ON c.id = a.customer_id
  LEFT JOIN account_movements m ON m.account_id = a.id
 GROUP BY a.id, c.id;

COMMENT ON VIEW vw_account_summary IS 'Cuentas con titular, número de movimientos y fecha del último movimiento.';

-- Volumen diario de transacciones por tipo y estado
CREATE OR REPLACE VIEW vw_daily_transactions AS
SELECT (t.created_at AT TIME ZONE 'UTC')::date AS business_date,
       t.transaction_type,
       t.status,
       t.currency,
       COUNT(*)                                 AS transactions_count,
       SUM(t.amount)                            AS total_amount
  FROM transactions t
 GROUP BY 1, t.transaction_type, t.status, t.currency;

COMMENT ON VIEW vw_daily_transactions IS 'Reporte diario (UTC) de cantidad y monto de transacciones.';

-- Permisos efectivos de cada usuario (solo roles activos)
CREATE OR REPLACE VIEW vw_user_permissions AS
SELECT u.id        AS user_id,
       u.username,
       u.status    AS user_status,
       r.code      AS role_code,
       p.code      AS permission_code,
       p.module
  FROM users u
  JOIN user_roles ur       ON ur.user_id = u.id
  JOIN roles r             ON r.id = ur.role_id AND r.active
  JOIN role_permissions rp ON rp.role_id = r.id
  JOIN permissions p       ON p.id = rp.permission_id;

COMMENT ON VIEW vw_user_permissions IS 'Permisos efectivos por usuario a través de sus roles activos.';
