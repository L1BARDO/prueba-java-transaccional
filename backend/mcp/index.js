#!/usr/bin/env node

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import pg from "pg";
import dotenv from "dotenv";

dotenv.config();

const { Pool } = pg;

// Configuración del pool de conexiones PostgreSQL con variables de entorno o valores por defecto
const pool = new Pool({
  host: process.env.DB_HOST || "localhost",
  port: parseInt(process.env.DB_PORT || "5432", 10),
  database: process.env.DB_NAME || "switch_transaccional",
  user: process.env.DB_USER || "postgres",
  password: process.env.DB_PASSWORD || "postgres",
  max: 10,
  idleTimeoutMillis: 30000,
});

pool.on("error", (err) => {
  console.error("[MCP-Postgres] Error en el pool de conexiones:", err.message);
});

// Inicialización del servidor MCP
const server = new McpServer({
  name: "switch-transaccional-db",
  version: "1.0.0",
});

/**
 * Función auxiliar para formatear la respuesta estándar de MCP
 */
function jsonResponse(data) {
  return {
    content: [
      {
        type: "text",
        text: JSON.stringify(data, null, 2),
      },
    ],
  };
}

/**
 * Herramienta: list_tables
 * Lista todas las tablas y vistas del esquema público.
 */
server.tool(
  "list_tables",
  "Lista todas las tablas y vistas disponibles en la base de datos con su tipo y estimación de filas.",
  {},
  async () => {
    const query = `
      SELECT 
        table_name,
        table_type,
        obj_description((quote_ident(table_schema) || '.' || quote_ident(table_name))::regclass, 'pg_class') AS description
      FROM information_schema.tables
      WHERE table_schema = 'public'
      ORDER BY table_type, table_name;
    `;
    const res = await pool.query(query);
    return jsonResponse({
      count: res.rows.length,
      tables: res.rows,
    });
  }
);

/**
 * Herramienta: describe_table
 * Devuelve la estructura de columnas, llaves y restricciones de una tabla.
 */
server.tool(
  "describe_table",
  "Describe la estructura de una tabla o vista (columnas, tipos de datos, nulabilidad y restricciones).",
  {
    table_name: z.string().describe("Nombre de la tabla o vista (ej: customers, accounts, transactions, account_movements)"),
  },
  async ({ table_name }) => {
    const colQuery = `
      SELECT 
        column_name,
        data_type,
        character_maximum_length,
        is_nullable,
        column_default
      FROM information_schema.columns
      WHERE table_schema = 'public' AND table_name = $1
      ORDER BY ordinal_position;
    `;
    const res = await pool.query(colQuery, [table_name.toLowerCase().trim()]);

    if (res.rows.length === 0) {
      return jsonResponse({
        error: `No se encontró la tabla o vista '${table_name}' en el esquema public.`,
      });
    }

    const constrQuery = `
      SELECT
        con.conname AS constraint_name,
        con.contype AS constraint_type,
        pg_get_constraintdef(con.oid) AS definition
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
      JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
      WHERE nsp.nspname = 'public' AND rel.relname = $1;
    `;
    const constrRes = await pool.query(constrQuery, [table_name.toLowerCase().trim()]);

    return jsonResponse({
      table: table_name,
      columns: res.rows,
      constraints: constrRes.rows,
    });
  }
);

/**
 * Herramienta: execute_query
 * Ejecuta consultas SQL en modo de sólo lectura (SELECT, WITH, EXPLAIN).
 */
server.tool(
  "execute_query",
  "Ejecuta una consulta SQL de sólo lectura (SELECT / WITH) sobre la base de datos switch_transaccional.",
  {
    sql: z.string().describe("Sentencia SQL de sólo lectura a ejecutar."),
    limit: z.number().optional().default(50).describe("Límite máximo de filas a retornar (máx 200)."),
  },
  async ({ sql, limit }) => {
    const trimmed = sql.trim();
    const upper = trimmed.toUpperCase();

    // Bloqueo de comandos que modifiquen el esquema o datos
    const forbidden = ["INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "GRANT", "REVOKE", "CREATE", "EXECUTE"];
    const firstWord = upper.split(/\s+/)[0];

    if (!["SELECT", "WITH", "EXPLAIN"].includes(firstWord)) {
      return jsonResponse({
        error: "Operación rechazada: solo se permiten sentencias de sólo lectura (SELECT, WITH, EXPLAIN).",
      });
    }

    for (const word of forbidden) {
      const regex = new RegExp(`\\b${word}\\b`, "i");
      if (regex.test(trimmed) && firstWord !== "SELECT" && firstWord !== "WITH" && firstWord !== "EXPLAIN") {
        return jsonResponse({
          error: `Operación rechazada: la consulta contiene la palabra clave restringida '${word}'.`,
        });
      }
    }

    const maxLimit = Math.min(Math.max(1, limit || 50), 200);

    const client = await pool.connect();
    try {
      await client.query("BEGIN TRANSACTION READ ONLY;");
      const res = await client.query(trimmed);
      await client.query("ROLLBACK;");

      const rows = res.rows.slice(0, maxLimit);
      return jsonResponse({
        rowCount: res.rowCount,
        returnedRows: rows.length,
        hasMore: res.rows.length > maxLimit,
        rows: rows,
      });
    } catch (err) {
      await client.query("ROLLBACK;");
      return jsonResponse({
        error: "Error ejecutando consulta SQL: " + err.message,
      });
    } finally {
      client.release();
    }
  }
);

/**
 * Herramienta: get_customer_overview
 * Obtiene la ficha consolidada de un cliente: datos personales, cuentas asociadas y saldo total.
 */
server.tool(
  "get_customer_overview",
  "Obtiene la ficha consolidada de un cliente: perfil, cuentas activas y saldo total por moneda.",
  {
    search: z.string().describe("Número de documento, correo electrónico o UUID del cliente."),
  },
  async ({ search }) => {
    const term = search.trim();
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(term);

    const customerQuery = isUuid
      ? "SELECT * FROM customers WHERE id = $1"
      : "SELECT * FROM customers WHERE document_number = $1 OR lower(email) = lower($1)";

    const custRes = await pool.query(customerQuery, [term]);
    if (custRes.rows.length === 0) {
      return jsonResponse({ error: `No se encontró ningún cliente con '${term}'` });
    }

    const customer = custRes.rows[0];

    const accountsRes = await pool.query(
      `SELECT id, account_number, account_type, currency, balance, status, created_at, updated_at
       FROM accounts
       WHERE customer_id = $1
       ORDER BY account_number ASC;`,
      [customer.id]
    );

    const totalBalanceRes = await pool.query(
      `SELECT currency, SUM(balance) AS total_balance
       FROM accounts
       WHERE customer_id = $1 AND status != 'CLOSED'
       GROUP BY currency;`,
      [customer.id]
    );

    return jsonResponse({
      customer,
      accountsCount: accountsRes.rows.length,
      totalBalancesByCurrency: totalBalanceRes.rows,
      accounts: accountsRes.rows,
    });
  }
);

/**
 * Herramienta: get_account_statement
 * Obtiene el extracto y últimos movimientos de una cuenta.
 */
server.tool(
  "get_account_statement",
  "Obtiene la información de una cuenta y sus últimos movimientos contables (ledger).",
  {
    account_identifier: z.string().describe("Número de cuenta de 10 dígitos o UUID de la cuenta."),
    limit: z.number().optional().default(20).describe("Número de movimientos recientes a consultar (por defecto 20)."),
  },
  async ({ account_identifier, limit }) => {
    const ident = account_identifier.trim();
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(ident);

    const accountQuery = isUuid
      ? "SELECT a.*, c.full_name AS customer_name, c.document_number FROM accounts a JOIN customers c ON c.id = a.customer_id WHERE a.id = $1"
      : "SELECT a.*, c.full_name AS customer_name, c.document_number FROM accounts a JOIN customers c ON c.id = a.customer_id WHERE a.account_number = $1";

    const accRes = await pool.query(accountQuery, [ident]);
    if (accRes.rows.length === 0) {
      return jsonResponse({ error: `No se encontró ninguna cuenta con '${ident}'` });
    }

    const account = accRes.rows[0];
    const maxLimit = Math.min(Math.max(1, limit || 20), 100);

    const stmtRes = await pool.query(
      "SELECT * FROM fn_account_statement($1, $2)",
      [account.id, maxLimit]
    );

    return jsonResponse({
      account: {
        id: account.id,
        account_number: account.account_number,
        customer_name: account.customer_name,
        document_number: account.document_number,
        type: account.account_type,
        currency: account.currency,
        current_balance: account.balance,
        status: account.status,
      },
      movementsCount: stmtRes.rows.length,
      movements: stmtRes.rows,
    });
  }
);

/**
 * Herramienta: get_transaction_details
 * Consulta el estado y detalle completo de una transacción por su referencia o UUID.
 */
server.tool(
  "get_transaction_details",
  "Consulta el detalle completo de una transacción (estado, cuentas, monto, error si fue rechazada y movimientos de ledger asociados).",
  {
    identifier: z.string().describe("Referencia de transacción (TRX-...), UUID de transacción o Idempotency-Key."),
  },
  async ({ identifier }) => {
    const ident = identifier.trim();
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(ident);

    const txQuery = isUuid
      ? `SELECT * FROM transactions WHERE id = $1 OR idempotency_key = $2`
      : `SELECT * FROM transactions WHERE reference = $1 OR idempotency_key = $1`;

    const params = isUuid ? [ident, ident] : [ident];
    const txRes = await pool.query(txQuery, params);

    if (txRes.rows.length === 0) {
      return jsonResponse({ error: `No se encontró ninguna transacción con el identificador '${ident}'` });
    }

    const transaction = txRes.rows[0];

    const movRes = await pool.query(
      `SELECT m.id, m.account_id, a.account_number, m.movement_type, m.amount, m.balance_after, m.created_at
       FROM account_movements m
       JOIN accounts a ON a.id = m.account_id
       WHERE m.transaction_id = $1
       ORDER BY m.created_at ASC;`,
      [transaction.id]
    );

    return jsonResponse({
      transaction,
      movements: movRes.rows,
    });
  }
);

/**
 * Herramienta: reconcile_balances
 * Ejecuta la función de base de datos fn_reconcile_balances() para verificar la consistencia contable.
 */
server.tool(
  "reconcile_balances",
  "Ejecuta la conciliación contable entre los saldos de las cuentas y la suma de sus movimientos de ledger.",
  {},
  async () => {
    const res = await pool.query("SELECT * FROM fn_reconcile_balances();");
    const isSynchronized = res.rows.length === 0;

    return jsonResponse({
      synchronized: isSynchronized,
      discrepancyCount: res.rows.length,
      status: isSynchronized ? "OK: Todos los saldos coinciden exactamente con el ledger." : "ALERTA: Se detectaron descuadres.",
      discrepancies: res.rows,
    });
  }
);

/**
 * Herramienta: get_daily_metrics
 * Consulta las métricas y volumen transaccional diario desde la vista vw_daily_transactions.
 */
server.tool(
  "get_daily_metrics",
  "Consulta las métricas agregadas de transacciones por día, tipo y estado.",
  {
    date: z.string().optional().describe("Fecha opcional en formato YYYY-MM-DD para filtrar el reporte."),
  },
  async ({ date }) => {
    let query = "SELECT * FROM vw_daily_transactions";
    const params = [];
    if (date) {
      query += " WHERE business_date = $1";
      params.push(date.trim());
    }
    query += " ORDER BY business_date DESC, transaction_type ASC;";

    const res = await pool.query(query, params);
    return jsonResponse({
      count: res.rows.length,
      metrics: res.rows,
    });
  }
);

// Conectar el servidor MCP vía Stdio
async function run() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("[MCP-Postgres] Servidor MCP switch-transaccional-db iniciado correctamente sobre Stdio");
}

run().catch((err) => {
  console.error("[MCP-Postgres] Error fatal al iniciar el servidor MCP:", err);
  process.exit(1);
});
