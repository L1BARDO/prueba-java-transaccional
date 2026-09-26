-- =====================================================================================
--  00 - Eliminación y creación de la base de datos
--  Se ejecuta conectado a la BD "postgres" (no se puede borrar la BD en la que se está conectado).
--  Variable psql requerida (la envía reset_database.bat / reset_database.sh): db_name
-- =====================================================================================

\echo '>> Eliminando base de datos' :db_name '(si existe)'
-- WITH (FORCE) cierra las conexiones activas (PostgreSQL 13+)
DROP DATABASE IF EXISTS :"db_name" WITH (FORCE);

\echo '>> Creando base de datos' :db_name
CREATE DATABASE :"db_name"
    WITH ENCODING = 'UTF8'
         TEMPLATE = template0;

COMMENT ON DATABASE :"db_name" IS 'Switch transaccional: clientes, cuentas, transacciones y seguridad';
