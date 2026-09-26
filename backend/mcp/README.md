# Servidor MCP para Base de Datos (Switch Transaccional)

Servidor compatible con la especificación [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) para conectar asistentes de IA (Google Antigravity, Claude Desktop, Cursor, etc.) directamente con la base de datos PostgreSQL de **Switch Transaccional**.

---

## Características

* **Conexión nativa a PostgreSQL**: Consulta clientes, cuentas, transacciones y el ledger contable.
* **Seguridad de solo lectura**: La herramienta `execute_query` ejecuta consultas en una transacción `READ ONLY` con rollback automático, evitando modificaciones involuntarias en los datos o auditorías.
* **Herramientas de negocio especializadas**:
  * Consultar extractos y ledger con `fn_account_statement`.
  * Conciliación automática de saldos contra el ledger con `fn_reconcile_balances`.
  * Búsqueda integral de clientes y consolidación de saldos por moneda.
  * Trazabilidad de transacciones por referencia, UUID o llave de idempotencia.
  * Inspección de esquema y estructura de tablas.

---

## Herramientas Disponibles (Tools)

| Herramienta | Parámetros | Descripción |
| :--- | :--- | :--- |
| `list_tables` | Ninguno | Lista todas las tablas y vistas del esquema `public`. |
| `describe_table` | `table_name` (string) | Estructura detallada: columnas, tipos de datos, nulabilidad y restricciones (PK, FK, CHECK, UK). |
| `execute_query` | `sql` (string), `limit` (opcional, default 50, máx 200) | Ejecuta consultas SQL de sólo lectura (`SELECT`, `WITH`, `EXPLAIN`). |
| `get_customer_overview` | `search` (string: documento, email o UUID) | Perfil del cliente, todas sus cuentas y saldo consolidado por divisa. |
| `get_account_statement` | `account_identifier` (número o UUID), `limit` (opcional) | Estado de la cuenta y sus últimos movimientos contables (asientos del ledger). |
| `get_transaction_details` | `identifier` (referencia `TRX-...`, UUID o Idempotency-Key) | Detalle de la transacción (estado, cuentas, monto, error si fue rechazada) y movimientos asociados. |
| `reconcile_balances` | Ninguno | Ejecuta `fn_reconcile_balances()` y reporta si todos los saldos de cuentas cuadran 100% con los asientos contables. |
| `get_daily_metrics` | `date` (opcional, formato `YYYY-MM-DD`) | Métricas agregadas de transacciones por fecha, tipo y estado desde `vw_daily_transactions`. |

---

## Variables de Entorno

Puedes configurar la conexión mediante variables de entorno o crear un archivo `.env` en este directorio:

```bash
DB_HOST=localhost
DB_PORT=5432
DB_NAME=switch_transaccional
DB_USER=postgres
DB_PASSWORD=postgres
```

---

## Uso

### Instalación de dependencias

```bash
cd backend/mcp
npm install
```

### Ejecución manual (Prueba por Stdio)

```bash
npm start
```

---

## Configuración en Clientes MCP

### 1. Google Antigravity

Edita o crea el archivo `~/.gemini/config/mcp_config.json`:

```json
{
  "mcpServers": {
    "switch-transaccional-db": {
      "command": "node",
      "args": ["/home/chico/Documentos/Java services/backend/mcp/index.js"],
      "env": {
        "DB_HOST": "localhost",
        "DB_PORT": "5432",
        "DB_NAME": "switch_transaccional",
        "DB_USER": "postgres",
        "DB_PASSWORD": "postgres"
      }
    }
  }
}
```

### 2. Claude Desktop (`claude_desktop_config.json`)

```json
{
  "mcpServers": {
    "switch-transaccional-db": {
      "command": "node",
      "args": ["/home/chico/Documentos/Java services/backend/mcp/index.js"],
      "env": {
        "DB_HOST": "localhost",
        "DB_PORT": "5432",
        "DB_NAME": "switch_transaccional",
        "DB_USER": "postgres",
        "DB_PASSWORD": "postgres"
      }
    }
  }
}
```

### 3. Cursor (`.cursor/mcp.json`)

```json
{
  "mcpServers": {
    "switch-transaccional-db": {
      "command": "node",
      "args": ["/home/chico/Documentos/Java services/backend/mcp/index.js"]
    }
  }
}
```
