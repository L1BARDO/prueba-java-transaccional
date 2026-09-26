#!/usr/bin/env bash
# =====================================================================================
#  reset_database.sh  (equivalente Linux/macOS de reset_database.bat)
#  Elimina y vuelve a crear la base de datos del Switch Transaccional.
#
#  Uso:
#      ./reset_database.sh              (estructura + datos de prueba)
#      ./reset_database.sh --sin-datos  (solo estructura)
#
#  Configuración por variables de entorno: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
# =====================================================================================
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-switch_transaccional}"
DB_USER="${DB_USER:-postgres}"
export PGPASSWORD="${DB_PASSWORD:-postgres}"
export PGCLIENTENCODING=UTF8
export PGOPTIONS="-c client_min_messages=warning"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PSQL=(psql -X -q -v ON_ERROR_STOP=1 -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER")

FILES=(01_creates.sql 02_indices.sql 03_funciones.sql 04_vistas.sql 05_inserts.sql)
if [[ "${1:-}" == "--sin-datos" ]]; then
    FILES=(01_creates.sql 02_indices.sql 03_funciones.sql 04_vistas.sql)
fi

command -v psql > /dev/null || { echo "[ERROR] No se encontró psql en el PATH" >&2; exit 1; }
trap 'echo; echo "[ERROR] Falló la ejecución de los scripts. Revise el mensaje anterior." >&2' ERR

echo "====================================================================="
echo " Reinicio de base de datos: $DB_NAME  ($DB_USER@$DB_HOST:$DB_PORT)"
echo " ATENCIÓN: se eliminarán TODOS los datos existentes."
echo "====================================================================="

echo "[1] 00_drop_create_database.sql"
"${PSQL[@]}" -d postgres -v db_name="$DB_NAME" -f "$SCRIPT_DIR/00_drop_create_database.sql"

step=2
for file in "${FILES[@]}" 99_verificacion.sql; do
    echo "[$step] $file"
    "${PSQL[@]}" -d "$DB_NAME" -f "$SCRIPT_DIR/$file"
    step=$((step + 1))
done

echo
echo "[OK] Base de datos $DB_NAME creada correctamente."
