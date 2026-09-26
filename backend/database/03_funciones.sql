-- =====================================================================================
--  03 - Funciones y triggers
-- =====================================================================================

-- -------------------------------------------------------------------------------------
--  fn_set_updated_at: mantiene updated_at en cada UPDATE
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

COMMENT ON FUNCTION fn_set_updated_at() IS 'Trigger: actualiza la columna updated_at en cada modificación.';

CREATE TRIGGER trg_customers_updated_at BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- -------------------------------------------------------------------------------------
--  fn_prevent_ledger_changes: transacciones y movimientos son inmutables (solo INSERT)
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_prevent_ledger_changes()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'La tabla % es inmutable: no se permite %', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'integrity_constraint_violation',
              HINT = 'Para corregir un movimiento registre una transacción compensatoria.';
END;
$$;

COMMENT ON FUNCTION fn_prevent_ledger_changes() IS 'Trigger: bloquea UPDATE/DELETE sobre el libro contable.';

CREATE TRIGGER trg_transactions_immutable BEFORE UPDATE OR DELETE ON transactions
    FOR EACH ROW EXECUTE FUNCTION fn_prevent_ledger_changes();

CREATE TRIGGER trg_movements_immutable BEFORE UPDATE OR DELETE ON account_movements
    FOR EACH ROW EXECUTE FUNCTION fn_prevent_ledger_changes();

-- -------------------------------------------------------------------------------------
--  fn_account_statement: extracto de una cuenta en un rango de fechas
--  Uso: SELECT * FROM fn_account_statement('<account_id>', now() - interval '30 days', now());
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_account_statement(
    p_account_id UUID,
    p_from       TIMESTAMPTZ DEFAULT NULL,
    p_to         TIMESTAMPTZ DEFAULT NULL
)
RETURNS TABLE (
    movement_date     TIMESTAMPTZ,
    reference         VARCHAR,
    transaction_type  VARCHAR,
    movement_type     VARCHAR,
    description       VARCHAR,
    debit             NUMERIC,
    credit            NUMERIC,
    balance_after     NUMERIC,
    currency          CHAR(3)
)
LANGUAGE sql
STABLE
AS $$
    SELECT m.created_at,
           t.reference,
           t.transaction_type,
           m.movement_type,
           t.description,
           CASE WHEN m.movement_type = 'DEBIT'  THEN m.amount END,
           CASE WHEN m.movement_type = 'CREDIT' THEN m.amount END,
           m.balance_after,
           m.currency
      FROM account_movements m
      JOIN transactions t ON t.id = m.transaction_id
     WHERE m.account_id = p_account_id
       AND (p_from IS NULL OR m.created_at >= p_from)
       AND (p_to   IS NULL OR m.created_at <= p_to)
     ORDER BY m.created_at, m.id;
$$;

COMMENT ON FUNCTION fn_account_statement(UUID, TIMESTAMPTZ, TIMESTAMPTZ) IS 'Extracto cronológico de una cuenta.';

-- -------------------------------------------------------------------------------------
--  fn_reconcile_balances: compara el saldo de cada cuenta contra su ledger.
--  Devuelve SOLO las cuentas descuadradas (resultado vacío = todo cuadra).
--  Uso: SELECT * FROM fn_reconcile_balances();
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_reconcile_balances()
RETURNS TABLE (
    account_id      UUID,
    account_number  VARCHAR,
    balance         NUMERIC,
    ledger_balance  NUMERIC,
    difference      NUMERIC
)
LANGUAGE sql
STABLE
AS $$
    SELECT a.id,
           a.account_number,
           a.balance,
           COALESCE(l.total, 0),
           a.balance - COALESCE(l.total, 0)
      FROM accounts a
      LEFT JOIN (
            SELECT m.account_id,
                   SUM(CASE WHEN m.movement_type = 'CREDIT' THEN m.amount ELSE -m.amount END) AS total
              FROM account_movements m
             GROUP BY m.account_id
      ) l ON l.account_id = a.id
     WHERE a.balance <> COALESCE(l.total, 0);
$$;

COMMENT ON FUNCTION fn_reconcile_balances() IS 'Conciliación: cuentas cuyo saldo no coincide con la suma de sus movimientos.';

-- -------------------------------------------------------------------------------------
--  fn_customer_total_balance: saldo consolidado de un cliente por moneda
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_customer_total_balance(p_customer_id UUID)
RETURNS TABLE (currency CHAR(3), total_balance NUMERIC, open_accounts BIGINT)
LANGUAGE sql
STABLE
AS $$
    SELECT a.currency, SUM(a.balance), COUNT(*)
      FROM accounts a
     WHERE a.customer_id = p_customer_id
       AND a.status <> 'CLOSED'
     GROUP BY a.currency
     ORDER BY a.currency;
$$;

COMMENT ON FUNCTION fn_customer_total_balance(UUID) IS 'Saldo total de las cuentas abiertas de un cliente, agrupado por moneda.';

-- -------------------------------------------------------------------------------------
--  fn_user_has_permission: ¿el usuario (activo) tiene el permiso por algún rol activo?
--  Uso: SELECT fn_user_has_permission('operador', 'TRANSACTION_TRANSFER');
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_user_has_permission(p_username VARCHAR, p_permission VARCHAR)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
          FROM users u
          JOIN user_roles ur       ON ur.user_id = u.id
          JOIN roles r             ON r.id = ur.role_id AND r.active
          JOIN role_permissions rp ON rp.role_id = r.id
          JOIN permissions p       ON p.id = rp.permission_id
         WHERE u.username = p_username
           AND u.status = 'ACTIVE'
           AND p.code = p_permission
    );
$$;

COMMENT ON FUNCTION fn_user_has_permission(VARCHAR, VARCHAR) IS 'Verifica si un usuario activo tiene un permiso.';

-- -------------------------------------------------------------------------------------
--  fn_register_failed_login: incrementa intentos fallidos y bloquea al llegar al máximo
--  Uso: SELECT fn_register_failed_login('operador', 5);
-- -------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_register_failed_login(p_username VARCHAR, p_max_attempts INTEGER DEFAULT 5)
RETURNS VARCHAR
LANGUAGE plpgsql
AS $$
DECLARE
    v_status VARCHAR;
BEGIN
    UPDATE users
       SET failed_login_attempts = failed_login_attempts + 1,
           status = CASE WHEN failed_login_attempts + 1 >= p_max_attempts THEN 'LOCKED' ELSE status END
     WHERE username = p_username
    RETURNING status INTO v_status;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe el usuario %', p_username USING ERRCODE = 'no_data_found';
    END IF;

    RETURN v_status;
END;
$$;

COMMENT ON FUNCTION fn_register_failed_login(VARCHAR, INTEGER) IS 'Registra un login fallido y bloquea el usuario al superar el máximo.';
