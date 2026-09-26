import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { CustomerService } from '../../core/services/customer.service';
import { AccountService } from '../../core/services/account.service';
import { TransactionService } from '../../core/services/transaction.service';
import { Account, Movement } from '../../core/models/models';

interface AccountMovementItem extends Movement {
  accountNumber?: string;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container">
      <!-- ========================================== -->
      <!-- CASO 1: VISTA DE CLIENTE (PORTAL PERSONAL) -->
      <!-- ========================================== -->
      <ng-container *ngIf="authService.isCustomer()">
        <!-- Banner de Bienvenida Personalizado -->
        <div class="customer-welcome">
          <div class="welcome-text">
            <span class="greeting-badge">Portal Clientes • Switch Banking</span>
            <h1>¡Hola, {{ authService.fullName() }}! 👋</h1>
            <p>Consulta tus saldos en tiempo real, administra tus tarjetas y realiza transferencias seguras.</p>
          </div>
          <div class="customer-badges">
            <span class="pill-badge pill-active">● Cuenta Verificada</span>
            <span class="pill-badge pill-role">Titular Financiero</span>
          </div>
        </div>

        <!-- Tarjetas de Saldo Consolidado -->
        <div class="client-metrics-grid">
          <div class="metric-card metric-primary">
            <span class="metric-title">Saldo Total (COP)</span>
            <span class="metric-value">{{ totalBalanceCop | currency:'COP':'symbol':'1.2-2' }} COP</span>
            <span class="metric-sub">{{ copAccountsCount }} {{ copAccountsCount === 1 ? 'cuenta' : 'cuentas' }} en Pesos Colombianos</span>
          </div>

          <div class="metric-card metric-secondary" *ngIf="usdAccountsCount > 0">
            <span class="metric-title">Saldo en Divisas (USD)</span>
            <span class="metric-value">{{ totalBalanceUsd | currency:'USD':'symbol':'1.2-2' }} USD</span>
            <span class="metric-sub">{{ usdAccountsCount }} {{ usdAccountsCount === 1 ? 'cuenta' : 'cuentas' }} en Dólares</span>
          </div>

          <div class="metric-card metric-neutral">
            <span class="metric-title">Cuentas Registradas</span>
            <span class="metric-value">{{ customerAccounts.length }}</span>
            <span class="metric-sub">
              {{ activeAccountsCount }} activas para transacciones
            </span>
          </div>

          <div class="metric-card metric-neutral">
            <span class="metric-title">Movimientos Registrados</span>
            <span class="metric-value">{{ recentMovements.length }}</span>
            <span class="metric-sub">Historial auditado en ledger</span>
          </div>
        </div>

        <!-- Fila Principal: Tarjeta Virtual + Gráficas de Saldo -->
        <div class="portal-main-grid">
          <!-- Columna 1: Tarjeta Débito Virtual Bancaria -->
          <div class="card virtual-card-panel">
            <div class="panel-header">
              <h2 class="panel-title">💳 Tu Tarjeta de Débito Virtual</h2>
              <span class="chip-status">Activa</span>
            </div>

            <div class="virtual-card" [class.usd-card]="selectedCardAccount?.currency === 'USD'">
              <div class="card-top">
                <div class="bank-brand">
                  <span class="brand-bolt">⚡</span> SWITCH <strong>PAY</strong>
                </div>
                <div class="contactless">
                  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M8.5 7.5c2.5-2 5.5-2 8 0m-6 3c1.5-1 3.5-1 5 0m-3.5 3c.5-.5 1.5-.5 2 0" />
                  </svg>
                </div>
              </div>

              <div class="card-chip">
                <div class="chip-line"></div>
                <div class="chip-line"></div>
              </div>

              <div class="card-number">
                •••• •••• •••• {{ getCardSuffix(selectedCardAccount?.accountNumber) }}
              </div>

              <div class="card-details">
                <div class="card-detail-item">
                  <span class="detail-label">TITULAR</span>
                  <span class="detail-val">{{ authService.fullName() | uppercase }}</span>
                </div>
                <div class="card-detail-item">
                  <span class="detail-label">VENCE</span>
                  <span class="detail-val">10 / 29</span>
                </div>
                <div class="card-detail-item">
                  <span class="detail-label">CUENTA</span>
                  <span class="detail-val">{{ selectedCardAccount?.type === 'SAVINGS' ? 'AHORROS' : 'CORRIENTE' }}</span>
                </div>
              </div>

              <div class="card-balance-tag">
                <span class="balance-tag-label">Saldo Disponible:</span>
                <strong class="balance-tag-val">
                  {{ selectedCardAccount?.balance | currency: selectedCardAccount?.currency : 'symbol' : '1.2-2' }} {{ selectedCardAccount?.currency }}
                </strong>
              </div>
            </div>

            <!-- Selector de Cuenta para la Tarjeta -->
            <div *ngIf="customerAccounts.length > 1" class="account-selector-row">
              <span class="selector-label">Cambiar cuenta de la tarjeta:</span>
              <div class="selector-chips">
                <button
                  *ngFor="let acc of customerAccounts"
                  type="button"
                  class="acc-chip"
                  [class.active]="selectedCardAccount?.id === acc.id"
                  (click)="selectedCardAccount = acc"
                >
                  {{ acc.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }} • {{ getCardSuffix(acc.accountNumber) }} ({{ acc.currency }})
                </button>
              </div>
            </div>
          </div>

          <!-- Columna 2: Gráficas de Saldo y Flujo Financiero -->
          <div class="card charts-panel">
            <div class="panel-header">
              <h2 class="panel-title">📊 Análisis de tu Patrimonio y Saldos</h2>
            </div>

            <!-- Gráfica 1: Distribución Porcentual por Cuenta -->
            <div class="chart-block">
              <span class="chart-subtitle">Distribución de Saldos por Cuenta</span>

              <div class="progress-bar-container">
                <div
                  *ngFor="let seg of balanceSegments"
                  class="progress-segment"
                  [style.width.%]="seg.percentage"
                  [style.background-color]="seg.color"
                  [title]="seg.label + ': ' + (seg.percentage | number:'1.1-1') + '%'"
                ></div>
              </div>

              <div class="legend-list">
                <div *ngFor="let seg of balanceSegments" class="legend-item">
                  <span class="legend-dot" [style.background-color]="seg.color"></span>
                  <span class="legend-text">
                    <strong>{{ seg.label }}</strong> ({{ seg.accountNumber }})
                  </span>
                  <span class="legend-amount">
                    {{ seg.balance | currency: seg.currency : 'symbol' : '1.2-2' }} {{ seg.currency }}
                    <small>({{ seg.percentage | number:'1.0-0' }}%)</small>
                  </span>
                </div>
              </div>
            </div>

            <hr class="chart-divider" />

            <!-- Gráfica 2: Flujo Financiero (Entradas vs Salidas) -->
            <div class="chart-block">
              <div class="flow-header">
                <span class="chart-subtitle">Flujo Reciente: Entradas vs Salidas</span>
                <span class="net-flow" [class.positive]="totalCredits >= totalDebits">
                  Flujo Neto: {{ (totalCredits - totalDebits) >= 0 ? '+' : '' }}{{ (totalCredits - totalDebits) | currency:'COP':'symbol':'1.0-0' }}
                </span>
              </div>

              <div class="flow-comparison">
                <div class="flow-col">
                  <div class="flow-bar-wrapper">
                    <div
                      class="flow-bar credits-bar"
                      [style.height.%]="getCreditsPercentage()"
                    ></div>
                  </div>
                  <span class="flow-val text-success">+{{ totalCredits | currency:'COP':'symbol':'1.0-0' }}</span>
                  <span class="flow-tag">📥 Entradas / Consignaciones</span>
                </div>

                <div class="flow-col">
                  <div class="flow-bar-wrapper">
                    <div
                      class="flow-bar debits-bar"
                      [style.height.%]="getDebitsPercentage()"
                    ></div>
                  </div>
                  <span class="flow-val text-danger">-{{ totalDebits | currency:'COP':'symbol':'1.0-0' }}</span>
                  <span class="flow-tag">📤 Salidas / Pagos / Retiros</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Fila Inferior: Movimientos Recientes del Cliente + Acciones Rápidas -->
        <div class="portal-bottom-grid">
          <!-- Movimientos Recientes -->
          <div class="card">
            <div class="card-header">
              <h2 class="card-title">📋 Tus Últimos Movimientos</h2>
              <a routerLink="/accounts" class="btn btn-outline btn-sm">Ver todos en extracto →</a>
            </div>

            <div class="table-container" *ngIf="recentMovements.length > 0">
              <table>
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Cuenta</th>
                    <th>Tipo</th>
                    <th>Monto</th>
                    <th>Saldo Posterior</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let m of recentMovements | slice:0:5">
                    <td>{{ m.createdAt | date:'short' }}</td>
                    <td><code>Nº {{ m.accountNumber }}</code></td>
                    <td>
                      <span
                        class="badge"
                        [ngClass]="m.type === 'CREDIT' ? 'badge-completed' : 'badge-rejected'"
                      >
                        {{ m.type === 'CREDIT' ? '📥 Crédito / Depósito' : '📤 Débito / Retiro' }}
                      </span>
                    </td>
                    <td [style.color]="m.type === 'CREDIT' ? 'var(--success)' : 'var(--danger)'" style="font-weight: 700;">
                      {{ m.type === 'CREDIT' ? '+' : '-' }}{{ m.amount | currency: m.currency : 'symbol' : '1.2-2' }} {{ m.currency }}
                    </td>
                    <td><strong>{{ m.balanceAfter | currency: m.currency : 'symbol' : '1.2-2' }}</strong></td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div *ngIf="recentMovements.length === 0" style="text-align: center; color: var(--gray-600); padding: 2rem;">
              No registras movimientos recientes en tus cuentas.
            </div>
          </div>

          <!-- Acciones Rápidas del Cliente -->
          <div class="card">
            <div class="card-header">
              <h2 class="card-title">⚡ Acciones Rápidas</h2>
            </div>
            <div class="quick-links-list">
              <a routerLink="/transactions" class="quick-link-item">
                <span class="quick-icon">🔄</span>
                <div>
                  <strong>Transferir Dinero</strong>
                  <p>Envía fondos de forma instantánea a cuentas en COP o USD.</p>
                </div>
              </a>
              <a routerLink="/accounts" class="quick-link-item">
                <span class="quick-icon">📄</span>
                <div>
                  <strong>Descargar Extractos</strong>
                  <p>Consulta el historial detallado de movimientos por cuenta.</p>
                </div>
              </a>
              <div class="security-tip-box">
                <span class="tip-icon">🛡️</span>
                <div>
                  <strong>Transacciones Seguras</strong>
                  <p>Tus transferencias están protegidas con llave de idempotencia criptográfica única.</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </ng-container>

      <!-- ======================================================== -->
      <!-- CASO 2: VISTA ADMINISTRATIVA (ADMIN, OPERADOR, AUDITOR)  -->
      <!-- ======================================================== -->
      <ng-container *ngIf="!authService.isCustomer()">
        <div class="welcome-banner">
          <div>
            <h1>Bienvenido, {{ authService.fullName() }}</h1>
            <p>Switch Transaccional con arquitectura hexagonal, balance en tiempo real y ledger inmutable.</p>
          </div>
          <div class="role-pills">
            <span class="role-badge" *ngFor="let role of authService.roles()">Rol: {{ role }}</span>
          </div>
        </div>

        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">👥</div>
            <div class="stat-body">
              <span class="stat-label">Clientes Registrados</span>
              <span class="stat-value">{{ totalCustomers }}</span>
            </div>
            <a *ngIf="authService.hasPermission('CUSTOMER_READ')" routerLink="/customers" class="stat-link">
              Gestionar clientes →
            </a>
          </div>

          <div class="stat-card">
            <div class="stat-icon">💳</div>
            <div class="stat-body">
              <span class="stat-label">Cuentas Bancarias</span>
              <span class="stat-value">{{ totalAccounts }}</span>
            </div>
            <a routerLink="/accounts" class="stat-link">Ver cuentas y saldos →</a>
          </div>

          <div class="stat-card">
            <div class="stat-icon">📊</div>
            <div class="stat-body">
              <span class="stat-label">Operaciones Procesadas</span>
              <span class="stat-value">{{ totalTransactions }}</span>
            </div>
            <a routerLink="/transactions" class="stat-link">Operar o auditar →</a>
          </div>
        </div>

        <div class="card">
          <div class="card-header">
            <h2 class="card-title">Acciones Rápidas del Switch</h2>
          </div>
          <div class="actions-grid">
            <a routerLink="/transactions" class="action-btn">
              <span class="action-icon">📥</span>
              <div>
                <strong>Realizar Depósito</strong>
                <p>Acredita fondos a una cuenta bancaria con registro en ledger contable.</p>
              </div>
            </a>

            <a routerLink="/transactions" class="action-btn">
              <span class="action-icon">📤</span>
              <div>
                <strong>Realizar Retiro</strong>
                <p>Debita saldo verificando disponibilidad y estado de la cuenta.</p>
              </div>
            </a>

            <a routerLink="/transactions" class="action-btn">
              <span class="action-icon">🔄</span>
              <div>
                <strong>Transferencia entre Cuentas</strong>
                <p>Movimiento atómico de fondos sin deadlocks mediante bloqueo pesimista ordenado.</p>
              </div>
            </a>

            <a routerLink="/accounts" class="action-btn">
              <span class="action-icon">📋</span>
              <div>
                <strong>Apertura de Cuenta</strong>
                <p>Emisión de número de cuenta de 10 dígitos y saldo inicial en cero.</p>
              </div>
            </a>
          </div>
        </div>

        <div class="features-grid">
          <div class="feature-box">
            <h3>🔒 Idempotencia Blindada</h3>
            <p>Cabecera <code>Idempotency-Key</code> obligatoria en operaciones de dinero para evitar doble débito o transacciones duplicadas ante fallos de red.</p>
          </div>
          <div class="feature-box">
            <h3>🛡️ Control de Concurrencia</h3>
            <p>Bloqueo pesimista (<code>PESSIMISTIC_WRITE</code>) ordenado cronológicamente por ID para prevenir deadlocks en transferencias concurrentes cruzadas.</p>
          </div>
          <div class="feature-box">
            <h3>📖 Auditoría y Ledger</h3>
            <p>Doble partida con movimientos inmutables (trigger PostgreSQL) y registro explícito de transacciones fallidas como <code>REJECTED</code>.</p>
          </div>
        </div>
      </ng-container>
    </div>
  `,
  styles: [`
    /* Vista Cliente */
    .customer-welcome {
      background: linear-gradient(135deg, #0f172a 0%, #1e3a8a 100%);
      color: white;
      padding: 2rem;
      border-radius: var(--radius);
      margin-bottom: 1.5rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1.25rem;
      box-shadow: 0 10px 25px -5px rgba(15, 23, 42, 0.25);
    }
    .greeting-badge {
      text-transform: uppercase;
      font-size: 0.75rem;
      letter-spacing: 1px;
      color: #93c5fd;
      font-weight: 700;
      display: block;
      margin-bottom: 0.4rem;
    }
    .customer-welcome h1 {
      font-size: 1.75rem;
      font-weight: 700;
      margin-bottom: 0.35rem;
    }
    .customer-welcome p {
      font-size: 0.95rem;
      color: #cbd5e1;
    }
    .customer-badges {
      display: flex;
      gap: 0.5rem;
    }
    .pill-badge {
      padding: 0.35rem 0.85rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
    }
    .pill-active {
      background: #166534;
      color: #86efac;
    }
    .pill-role {
      background: rgba(255, 255, 255, 0.15);
      color: white;
    }

    .client-metrics-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 1rem;
      margin-bottom: 1.5rem;
    }
    .metric-card {
      background: white;
      border: 1px solid var(--gray-200);
      border-radius: var(--radius);
      padding: 1.25rem;
      display: flex;
      flex-direction: column;
      box-shadow: var(--shadow);
      border-top: 4px solid var(--gray-300);
    }
    .metric-primary {
      border-top-color: #1d4ed8;
      background: linear-gradient(180deg, #eff6ff 0%, #ffffff 40%);
    }
    .metric-secondary {
      border-top-color: #059669;
      background: linear-gradient(180deg, #ecfdf5 0%, #ffffff 40%);
    }
    .metric-title {
      font-size: 0.8rem;
      color: var(--gray-600);
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .metric-value {
      font-size: 1.6rem;
      font-weight: 800;
      color: var(--gray-900);
      margin: 0.35rem 0;
    }
    .metric-sub {
      font-size: 0.775rem;
      color: var(--gray-600);
    }

    .portal-main-grid {
      display: grid;
      grid-template-columns: 1fr 1.3fr;
      gap: 1.5rem;
      margin-bottom: 1.5rem;
    }
    @media (max-width: 900px) {
      .portal-main-grid {
        grid-template-columns: 1fr;
      }
    }

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }
    .panel-title {
      font-size: 1.1rem;
      font-weight: 700;
      color: var(--gray-900);
    }
    .chip-status {
      font-size: 0.75rem;
      background: #dcfce7;
      color: #15803d;
      padding: 0.2rem 0.5rem;
      border-radius: 9999px;
      font-weight: 600;
    }

    /* Tarjeta Virtual */
    .virtual-card {
      width: 100%;
      height: 220px;
      border-radius: 16px;
      background: linear-gradient(135deg, #1e3a8a 0%, #0f172a 60%, #1e1b4b 100%);
      color: white;
      padding: 1.5rem;
      position: relative;
      box-shadow: 0 15px 30px -10px rgba(30, 58, 138, 0.5);
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      overflow: hidden;
      border: 1px solid rgba(255, 255, 255, 0.15);
    }
    .virtual-card.usd-card {
      background: linear-gradient(135deg, #065f46 0%, #064e3b 60%, #022c22 100%);
      box-shadow: 0 15px 30px -10px rgba(6, 95, 70, 0.5);
    }
    .card-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .bank-brand {
      font-size: 1rem;
      letter-spacing: 1px;
      display: flex;
      align-items: center;
      gap: 0.35rem;
    }
    .card-chip {
      width: 44px;
      height: 32px;
      background: linear-gradient(135deg, #fcd34d 0%, #d97706 100%);
      border-radius: 6px;
      position: relative;
      margin: 0.5rem 0;
      display: flex;
      flex-direction: column;
      justify-content: space-around;
      padding: 4px;
      box-shadow: inset 0 0 4px rgba(0, 0, 0, 0.2);
    }
    .chip-line {
      height: 1px;
      background: rgba(0, 0, 0, 0.3);
    }
    .card-number {
      font-family: 'Courier New', Courier, monospace;
      font-size: 1.25rem;
      letter-spacing: 3px;
      font-weight: 700;
      color: #f8fafc;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
    }
    .card-details {
      display: flex;
      gap: 1.5rem;
    }
    .card-detail-item {
      display: flex;
      flex-direction: column;
    }
    .detail-label {
      font-size: 0.6rem;
      color: #94a3b8;
      letter-spacing: 0.5px;
    }
    .detail-val {
      font-size: 0.8rem;
      font-weight: 700;
      letter-spacing: 0.5px;
    }
    .card-balance-tag {
      position: absolute;
      right: 1.5rem;
      bottom: 1.25rem;
      text-align: right;
      background: rgba(255, 255, 255, 0.12);
      backdrop-filter: blur(8px);
      padding: 0.35rem 0.75rem;
      border-radius: 8px;
      border: 1px solid rgba(255, 255, 255, 0.2);
    }
    .balance-tag-label {
      display: block;
      font-size: 0.65rem;
      color: #cbd5e1;
    }
    .balance-tag-val {
      font-size: 0.95rem;
      color: #ffffff;
    }
    .account-selector-row {
      margin-top: 1rem;
      padding-top: 0.75rem;
      border-top: 1px solid var(--gray-200);
    }
    .selector-label {
      display: block;
      font-size: 0.75rem;
      color: var(--gray-600);
      margin-bottom: 0.35rem;
    }
    .selector-chips {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
    }
    .acc-chip {
      padding: 0.35rem 0.65rem;
      border-radius: 6px;
      border: 1px solid var(--gray-300);
      background: var(--gray-100);
      font-size: 0.775rem;
      font-weight: 600;
      color: var(--gray-700);
      cursor: pointer;
      transition: all 0.15s;
    }
    .acc-chip.active {
      border-color: var(--primary);
      background: var(--primary-light);
      color: var(--primary);
    }

    /* Gráficas Panel */
    .chart-block {
      margin-bottom: 0.75rem;
    }
    .chart-subtitle {
      font-size: 0.85rem;
      font-weight: 700;
      color: var(--gray-700);
      display: block;
      margin-bottom: 0.65rem;
    }
    .progress-bar-container {
      height: 14px;
      width: 100%;
      background: var(--gray-100);
      border-radius: 9999px;
      overflow: hidden;
      display: flex;
      margin-bottom: 0.75rem;
      box-shadow: inset 0 1px 3px rgba(0, 0, 0, 0.1);
    }
    .progress-segment {
      height: 100%;
      transition: width 0.4s ease-in-out;
    }
    .legend-list {
      display: flex;
      flex-direction: column;
      gap: 0.4rem;
    }
    .legend-item {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.8rem;
    }
    .legend-dot {
      width: 10px;
      height: 10px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .legend-text {
      flex: 1;
      color: var(--gray-700);
    }
    .legend-amount {
      font-weight: 700;
      color: var(--gray-900);
    }
    .chart-divider {
      border: 0;
      border-top: 1px solid var(--gray-200);
      margin: 1rem 0;
    }
    .flow-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.75rem;
    }
    .net-flow {
      font-size: 0.75rem;
      font-weight: 700;
      padding: 0.2rem 0.5rem;
      border-radius: 4px;
      background: #fee2e2;
      color: #b91c1c;
    }
    .net-flow.positive {
      background: #dcfce7;
      color: #15803d;
    }
    .flow-comparison {
      display: flex;
      justify-content: space-around;
      align-items: flex-end;
      height: 110px;
      padding: 0.5rem 0;
    }
    .flow-col {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.35rem;
      width: 45%;
    }
    .flow-bar-wrapper {
      width: 48px;
      height: 60px;
      background: var(--gray-100);
      border-radius: 6px;
      display: flex;
      align-items: flex-end;
      overflow: hidden;
    }
    .flow-bar {
      width: 100%;
      transition: height 0.4s ease;
      border-radius: 6px 6px 0 0;
    }
    .credits-bar {
      background: #22c55e;
    }
    .debits-bar {
      background: #ef4444;
    }
    .flow-val {
      font-size: 0.85rem;
      font-weight: 700;
    }
    .flow-tag {
      font-size: 0.7rem;
      color: var(--gray-600);
      text-align: center;
    }
    .text-success { color: #16a34a; }
    .text-danger { color: #dc2626; }

    /* Bottom Grid */
    .portal-bottom-grid {
      display: grid;
      grid-template-columns: 1.4fr 1fr;
      gap: 1.5rem;
    }
    @media (max-width: 900px) {
      .portal-bottom-grid {
        grid-template-columns: 1fr;
      }
    }
    .quick-links-list {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }
    .quick-link-item {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.85rem 1rem;
      border: 1px solid var(--gray-200);
      border-radius: var(--radius);
      color: inherit;
      transition: all 0.2s;
    }
    .quick-link-item:hover {
      border-color: var(--primary);
      background: var(--primary-light);
      text-decoration: none;
      transform: translateX(3px);
    }
    .quick-icon {
      font-size: 1.5rem;
    }
    .quick-link-item strong {
      display: block;
      color: var(--primary);
      font-size: 0.9rem;
    }
    .quick-link-item p {
      font-size: 0.775rem;
      color: var(--gray-600);
      margin: 0;
    }
    .security-tip-box {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      background: #f8fafc;
      border: 1px dashed var(--gray-300);
      padding: 0.75rem;
      border-radius: var(--radius);
      font-size: 0.775rem;
      color: var(--gray-700);
    }
    .tip-icon {
      font-size: 1.25rem;
    }

    /* Vista Admin */
    .welcome-banner {
      background: linear-gradient(135deg, var(--primary) 0%, #1e40af 100%);
      color: white;
      padding: 2rem;
      border-radius: var(--radius);
      margin-bottom: 2rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .welcome-banner h1 {
      font-size: 1.6rem;
      margin-bottom: 0.25rem;
    }
    .welcome-banner p {
      font-size: 0.95rem;
      opacity: 0.9;
    }
    .role-badge {
      background: rgba(255, 255, 255, 0.2);
      padding: 0.35rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.8rem;
      font-weight: 600;
    }
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 1.5rem;
      margin-bottom: 2rem;
    }
    .stat-card {
      background: white;
      border: 1px solid var(--gray-200);
      border-radius: var(--radius);
      padding: 1.25rem;
      display: flex;
      flex-direction: column;
      box-shadow: var(--shadow);
    }
    .stat-icon {
      font-size: 1.75rem;
      margin-bottom: 0.5rem;
    }
    .stat-label {
      font-size: 0.85rem;
      color: var(--gray-600);
      font-weight: 500;
    }
    .stat-value {
      font-size: 1.75rem;
      font-weight: 700;
      color: var(--gray-900);
      display: block;
      margin-bottom: 0.75rem;
    }
    .stat-link {
      font-size: 0.85rem;
      font-weight: 500;
      margin-top: auto;
    }
    .actions-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 1rem;
    }
    .action-btn {
      display: flex;
      gap: 1rem;
      padding: 1rem;
      border: 1px solid var(--gray-200);
      border-radius: var(--radius);
      color: inherit;
      transition: all 0.2s;
    }
    .action-btn:hover {
      border-color: var(--primary);
      background-color: var(--primary-light);
      text-decoration: none;
      transform: translateY(-2px);
    }
    .action-icon {
      font-size: 2rem;
    }
    .action-btn strong {
      display: block;
      color: var(--primary);
      margin-bottom: 0.25rem;
    }
    .action-btn p {
      font-size: 0.8rem;
      color: var(--gray-600);
      line-height: 1.3;
    }
    .features-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 1.25rem;
    }
    .feature-box {
      background: white;
      border: 1px solid var(--gray-200);
      border-radius: var(--radius);
      padding: 1.25rem;
    }
    .feature-box h3 {
      font-size: 1rem;
      margin-bottom: 0.5rem;
      color: var(--gray-900);
    }
    .feature-box p {
      font-size: 0.85rem;
      color: var(--gray-600);
      line-height: 1.4;
    }
    .feature-box code {
      background: var(--gray-100);
      padding: 0.15rem 0.35rem;
      border-radius: 4px;
      font-size: 0.8rem;
    }
  `]
})
export class DashboardComponent implements OnInit {
  // Datos Administrativos
  totalCustomers = 0;
  totalAccounts = 0;
  totalTransactions = 0;

  // Datos para Cliente
  customerAccounts: Account[] = [];
  selectedCardAccount: Account | null = null;
  totalBalanceCop = 0;
  totalBalanceUsd = 0;
  copAccountsCount = 0;
  usdAccountsCount = 0;
  activeAccountsCount = 0;

  balanceSegments: {
    label: string;
    accountNumber: string;
    balance: number;
    currency: string;
    percentage: number;
    color: string;
  }[] = [];

  recentMovements: AccountMovementItem[] = [];
  totalCredits = 0;
  totalDebits = 0;

  private readonly segmentColors = ['#1d4ed8', '#059669', '#d97706', '#7c3aed', '#db2777'];

  constructor(
    public authService: AuthService,
    private customerService: CustomerService,
    private accountService: AccountService,
    private transactionService: TransactionService
  ) {}

  ngOnInit(): void {
    if (this.authService.isCustomer()) {
      this.loadCustomerData();
    } else {
      this.loadAdminData();
    }
  }

  loadAdminData(): void {
    if (this.authService.hasPermission('CUSTOMER_READ')) {
      this.customerService.getAll(0, 1).subscribe({
        next: (res) => (this.totalCustomers = res.totalElements),
        error: () => {}
      });
    }

    this.accountService.getAll(undefined, undefined, 0, 1).subscribe({
      next: (res) => (this.totalAccounts = res.totalElements),
      error: () => {}
    });

    this.transactionService.search({}, 0, 1).subscribe({
      next: (res) => (this.totalTransactions = res.totalElements),
      error: () => {}
    });
  }

  loadCustomerData(): void {
    const custId = this.authService.customerId();

    this.accountService.getAll(custId || undefined, undefined, 0, 50).subscribe({
      next: (res) => {
        this.customerAccounts = res.content;
        this.activeAccountsCount = this.customerAccounts.filter((a) => a.status === 'ACTIVE').length;

        if (this.customerAccounts.length > 0) {
          this.selectedCardAccount = this.customerAccounts[0];
          this.calculateCustomerMetrics();
          this.loadCustomerMovements();
        }
      }
    });
  }

  calculateCustomerMetrics(): void {
    let copSum = 0;
    let usdSum = 0;
    let copCount = 0;
    let usdCount = 0;

    for (const acc of this.customerAccounts) {
      if (acc.currency === 'COP') {
        copSum += acc.balance;
        copCount++;
      } else if (acc.currency === 'USD') {
        usdSum += acc.balance;
        usdCount++;
      }
    }

    this.totalBalanceCop = copSum;
    this.totalBalanceUsd = usdSum;
    this.copAccountsCount = copCount;
    this.usdAccountsCount = usdCount;

    // Calcular distribución porcentual
    // Para simplificar la visualización de la barra, normalizamos sumando el balance en COP + (USD * 4000 aprox) o relativo
    const normalizedBalances = this.customerAccounts.map((a) => {
      const weight = a.currency === 'USD' ? a.balance * 4000 : a.balance;
      return { acc: a, weight };
    });

    const totalWeight = normalizedBalances.reduce((acc, curr) => acc + curr.weight, 0) || 1;

    this.balanceSegments = normalizedBalances.map((item, index) => {
      const percentage = (item.weight / totalWeight) * 100;
      return {
        label: item.acc.type === 'SAVINGS' ? 'Cuenta de Ahorros' : 'Cuenta Corriente',
        accountNumber: item.acc.accountNumber,
        balance: item.acc.balance,
        currency: item.acc.currency,
        percentage: Math.max(percentage, 5), // al menos 5% para que sea visible
        color: this.segmentColors[index % this.segmentColors.length]
      };
    });
  }

  loadCustomerMovements(): void {
    const movementCalls = this.customerAccounts.map((a) =>
      this.accountService.getMovements(a.id, 0, 10)
    );

    if (movementCalls.length === 0) return;

    forkJoin(movementCalls).subscribe({
      next: (results) => {
        const allMovements: AccountMovementItem[] = [];
        let credits = 0;
        let debits = 0;

        results.forEach((res, index) => {
          const accNumber = this.customerAccounts[index]?.accountNumber;
          res.content.forEach((m) => {
            allMovements.push({ ...m, accountNumber: accNumber });
            if (m.type === 'CREDIT') {
              credits += (m.currency === 'USD' ? m.amount * 4000 : m.amount);
            } else {
              debits += (m.currency === 'USD' ? m.amount * 4000 : m.amount);
            }
          });
        });

        // Ordenar descendentemente por fecha
        allMovements.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());

        this.recentMovements = allMovements;
        this.totalCredits = credits;
        this.totalDebits = debits;
      }
    });
  }

  getCardSuffix(accountNumber?: string): string {
    if (!accountNumber) return '0000';
    return accountNumber.length > 4 ? accountNumber.slice(-4) : accountNumber;
  }

  getCreditsPercentage(): number {
    const max = Math.max(this.totalCredits, this.totalDebits, 1);
    return Math.min(Math.round((this.totalCredits / max) * 100), 100);
  }

  getDebitsPercentage(): number {
    const max = Math.max(this.totalCredits, this.totalDebits, 1);
    return Math.min(Math.round((this.totalDebits / max) * 100), 100);
  }
}
