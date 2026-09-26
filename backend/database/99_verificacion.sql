-- =====================================================================================
--  99 - Verificación posterior a la carga
-- =====================================================================================

\echo ''
\echo '>> Registros por tabla'
SELECT 'customers' AS tabla, COUNT(*) AS registros FROM customers
UNION ALL SELECT 'accounts',          COUNT(*) FROM accounts
UNION ALL SELECT 'transactions',      COUNT(*) FROM transactions
UNION ALL SELECT 'account_movements', COUNT(*) FROM account_movements
UNION ALL SELECT 'users',             COUNT(*) FROM users
UNION ALL SELECT 'roles',             COUNT(*) FROM roles
UNION ALL SELECT 'permissions',       COUNT(*) FROM permissions
UNION ALL SELECT 'role_permissions',  COUNT(*) FROM role_permissions
UNION ALL SELECT 'user_roles',        COUNT(*) FROM user_roles;

\echo '>> Conciliación saldo vs ledger (debe decir: 0 cuentas descuadradas)'
SELECT COUNT(*) AS cuentas_descuadradas FROM fn_reconcile_balances();
