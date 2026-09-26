import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransactionService } from '../../core/services/transaction.service';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Account, CurrencyCode, Transaction } from '../../core/models/models';

@Component({
  selector: 'app-transaction-operations',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container">
      <div class="card">
        <div class="card-header">
          <div>
            <h1 class="card-title">
              {{ authService.isCustomer() ? 'Transferencias y Movimientos' : 'Motor Transaccional' }}
            </h1>
            <p style="font-size: 0.85rem; color: var(--gray-600);">
              {{
                authService.isCustomer()
                  ? 'Envía dinero de forma atómica y consulta el estado de tus operaciones bancarias.'
                  : 'Depósitos, retiros y transferencias atómicas con idempotencia garantizada.'
              }}
            </p>
          </div>
        </div>

        <div class="tabs">
          <!-- Pestañas solo para operadores / cajeros -->
          <button
            *ngIf="!authService.isCustomer() && authService.hasPermission('TRANSACTION_DEPOSIT')"
            class="tab-btn"
            [class.active]="activeTab === 'DEPOSIT'"
            (click)="setTab('DEPOSIT')"
          >
            📥 Depósito
          </button>
          <button
            *ngIf="!authService.isCustomer() && authService.hasPermission('TRANSACTION_WITHDRAW')"
            class="tab-btn"
            [class.active]="activeTab === 'WITHDRAWAL'"
            (click)="setTab('WITHDRAWAL')"
          >
            📤 Retiro
          </button>

          <!-- Pestañas comunes -->
          <button
            *ngIf="authService.hasPermission('TRANSACTION_TRANSFER')"
            class="tab-btn"
            [class.active]="activeTab === 'TRANSFER'"
            (click)="setTab('TRANSFER')"
          >
            🔄 Transferencia
          </button>
          <button
            *ngIf="authService.hasPermission('TRANSACTION_READ')"
            class="tab-btn"
            [class.active]="activeTab === 'HISTORY'"
            (click)="setTab('HISTORY')"
          >
            {{ authService.isCustomer() ? '📋 Mis Transacciones' : '📋 Historial Global' }}
          </button>
        </div>

        <!-- Comprobante de última transacción exitosa -->
        <div *ngIf="lastTransaction" class="voucher-box">
          <div class="voucher-header">
            <span class="voucher-title">
              <span class="voucher-icon">✓</span> Comprobante de Operación Exitosa
            </span>
            <button
              type="button"
              class="voucher-close"
              (click)="lastTransaction = null"
              title="Cerrar comprobante"
            >
              ✕
            </button>
          </div>
          <div class="voucher-body">
            <div class="voucher-item">
              <span class="voucher-label">Referencia</span>
              <span class="voucher-value"><code>{{ lastTransaction.reference }}</code></span>
            </div>
            <div class="voucher-item">
              <span class="voucher-label">Tipo</span>
              <span class="voucher-value"><strong>{{ lastTransaction.type }}</strong></span>
            </div>
            <div class="voucher-item">
              <span class="voucher-label">Monto</span>
              <span class="voucher-value font-highlight">
                {{ lastTransaction.amount | currency: lastTransaction.currency : 'symbol' : '1.2-2' }} {{ lastTransaction.currency }}
              </span>
            </div>
            <div class="voucher-item">
              <span class="voucher-label">Estado</span>
              <span class="voucher-value">
                <span class="badge badge-completed">{{ lastTransaction.status }}</span>
              </span>
            </div>
          </div>
        </div>

        <!-- TAB: DEPÓSITO (solo operadores) -->
        <div *ngIf="activeTab === 'DEPOSIT' && authService.hasPermission('TRANSACTION_DEPOSIT')" class="tab-content">
          <form (ngSubmit)="submitDeposit()">
            <div class="form-group">
              <label class="form-label">Cuenta Destino</label>
              <select
                class="form-control"
                [(ngModel)]="depositForm.accountId"
                name="depAccount"
                required
                (change)="onDepositAccountChange()"
              >
                <option value="" disabled>Seleccione cuenta destino</option>
                <option *ngFor="let a of activeAccounts" [value]="a.id">
                  Cuenta {{ a.accountNumber }} - Saldo: {{ a.balance | currency: a.currency }} ({{ a.currency }})
                </option>
              </select>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Monto a Depositar</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  class="form-control"
                  [(ngModel)]="depositForm.amount"
                  name="depAmount"
                  placeholder="0.00"
                  required
                />
              </div>

              <div class="form-group">
                <label class="form-label">Moneda</label>
                <input
                  type="text"
                  class="form-control"
                  [value]="depositForm.currency"
                  disabled
                  style="background: var(--gray-100);"
                />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">Descripción / Concepto</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="depositForm.description"
                name="depDesc"
                placeholder="Ej: Depósito en ventanilla"
                required
              />
            </div>

            <button type="submit" class="btn btn-primary" [disabled]="submitting">
              {{ submitting ? 'Procesando...' : 'Aplicar Depósito' }}
            </button>
          </form>
        </div>

        <!-- TAB: RETIRO (solo operadores) -->
        <div *ngIf="activeTab === 'WITHDRAWAL' && authService.hasPermission('TRANSACTION_WITHDRAW')" class="tab-content">
          <form (ngSubmit)="submitWithdrawal()">
            <div class="form-group">
              <label class="form-label">Cuenta Origen</label>
              <select
                class="form-control"
                [(ngModel)]="withdrawalForm.accountId"
                name="wAccount"
                required
                (change)="onWithdrawAccountChange()"
              >
                <option value="" disabled>Seleccione cuenta origen</option>
                <option *ngFor="let a of activeAccounts" [value]="a.id">
                  Cuenta {{ a.accountNumber }} - Saldo Disponible: {{ a.balance | currency: a.currency }} ({{ a.currency }})
                </option>
              </select>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Monto a Retirar</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  class="form-control"
                  [(ngModel)]="withdrawalForm.amount"
                  name="wAmount"
                  placeholder="0.00"
                  required
                />
              </div>

              <div class="form-group">
                <label class="form-label">Moneda</label>
                <input
                  type="text"
                  class="form-control"
                  [value]="withdrawalForm.currency"
                  disabled
                  style="background: var(--gray-100);"
                />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">Descripción / Concepto</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="withdrawalForm.description"
                name="wDesc"
                placeholder="Ej: Retiro en cajero automático"
                required
              />
            </div>

            <button type="submit" class="btn btn-primary" [disabled]="submitting">
              {{ submitting ? 'Procesando...' : 'Aplicar Retiro' }}
            </button>
          </form>
        </div>

        <!-- TAB: TRANSFERENCIA (disponible para todos con permiso) -->
        <div *ngIf="activeTab === 'TRANSFER' && authService.hasPermission('TRANSACTION_TRANSFER')" class="tab-content">
          <form (ngSubmit)="submitTransfer()">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Cuenta Origen (Débito)</label>
                <select
                  class="form-control"
                  [(ngModel)]="transferForm.sourceAccountId"
                  name="trSource"
                  required
                  (change)="onTransferSourceChange()"
                >
                  <option value="" disabled>Seleccione cuenta origen</option>
                  <option *ngFor="let a of myActiveAccounts" [value]="a.id">
                    Cuenta {{ a.accountNumber }} ({{ a.currency }}) - Saldo: {{ a.balance | currency: a.currency }}
                  </option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Cuenta Destino (Crédito)</label>
                <select
                  class="form-control"
                  [(ngModel)]="transferForm.destinationAccountId"
                  name="trDest"
                  required
                >
                  <option value="" disabled>Seleccione cuenta destino</option>
                  <option
                    *ngFor="let a of getEligibleDestinationAccounts()"
                    [value]="a.id"
                  >
                    Cuenta {{ a.accountNumber }} ({{ a.currency }}) - {{ a.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}
                  </option>
                </select>
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Monto a Transferir</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  class="form-control"
                  [(ngModel)]="transferForm.amount"
                  name="trAmount"
                  placeholder="0.00"
                  required
                />
              </div>

              <div class="form-group">
                <label class="form-label">Moneda</label>
                <input
                  type="text"
                  class="form-control"
                  [value]="transferForm.currency"
                  disabled
                  style="background: var(--gray-100);"
                />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">Descripción</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="transferForm.description"
                name="trDesc"
                placeholder="Ej: Pago de servicios / Transferencia entre cuentas"
                required
              />
            </div>

            <button type="submit" class="btn btn-primary" [disabled]="submitting">
              {{ submitting ? 'Procesando...' : 'Ejecutar Transferencia Atómica' }}
            </button>
          </form>
        </div>

        <!-- TAB: HISTORIAL CON SCROLL INFINITO (8 registros por página) -->
        <div *ngIf="activeTab === 'HISTORY' && authService.hasPermission('TRANSACTION_READ')" class="tab-content">
          <!-- Filtro de cuenta específico para clientes -->
          <div *ngIf="authService.isCustomer() && myAccounts.length > 0" class="filter-box-client">
            <label class="form-label" style="margin-bottom: 0;">Filtrar por mi cuenta:</label>
            <select
              class="form-control"
              style="max-width: 380px;"
              [(ngModel)]="filterAccountId"
              (change)="loadTransactions(true)"
            >
              <option value="">-- Todas mis cuentas (Ahorros y Corrientes) --</option>
              <option *ngFor="let a of myAccounts" [value]="a.id">
                Cuenta {{ a.accountNumber }} ({{ a.currency }}) - {{ a.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}
              </option>
            </select>
          </div>

          <div class="history-meta">
            <span class="history-count">
              Mostrando <strong>{{ transactions.length }}</strong> de <strong>{{ totalElements }}</strong> transacciones registradas
            </span>
            <span class="history-hint">
              ↕ Desplaza verticalmente la tabla para cargar más registros automáticamente
            </span>
          </div>

          <div class="history-scroll-container" (scroll)="onHistoryScroll($event)">
            <table>
              <thead>
                <tr>
                  <th class="sticky-th">Referencia</th>
                  <th class="sticky-th">Tipo</th>
                  <th *ngIf="authService.isCustomer()" class="sticky-th">Sentido</th>
                  <th class="sticky-th">Monto</th>
                  <th class="sticky-th">Moneda</th>
                  <th class="sticky-th">Estado</th>
                  <th class="sticky-th">Fecha</th>
                  <th class="sticky-th">Detalle / Razón</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let tx of transactions">
                  <td><code>{{ tx.reference }}</code></td>
                  <td><strong>{{ tx.type }}</strong></td>
                  <td *ngIf="authService.isCustomer()">
                    <span class="movement-pill" [ngClass]="getTransactionDirectionClass(tx)">
                      {{ getTransactionDirectionLabel(tx) }}
                    </span>
                  </td>
                  <td style="font-weight: 600;">
                    {{ tx.amount | currency: tx.currency : 'symbol' : '1.2-2' }}
                  </td>
                  <td>{{ tx.currency }}</td>
                  <td>
                    <span
                      class="badge"
                      [ngClass]="tx.status === 'COMPLETED' ? 'badge-completed' : 'badge-rejected'"
                    >
                      {{ tx.status }}
                    </span>
                  </td>
                  <td>{{ tx.createdAt | date:'short' }}</td>
                  <td style="font-size: 0.8rem; color: var(--gray-600);">
                    <span *ngIf="tx.status === 'REJECTED'" style="color: var(--danger);">
                      <strong>{{ tx.failureCode }}</strong>: {{ tx.failureReason }}
                    </span>
                    <span *ngIf="tx.status === 'COMPLETED'">
                      {{ tx.description || 'Procesada con éxito' }}
                    </span>
                  </td>
                </tr>

                <tr *ngIf="loadingInitial">
                  <td [attr.colspan]="authService.isCustomer() ? 8 : 7" style="text-align: center; color: var(--gray-600); padding: 2.5rem;">
                    <div class="spinner-line">Cargando transacciones...</div>
                  </td>
                </tr>

                <tr *ngIf="!loadingInitial && transactions.length === 0">
                  <td [attr.colspan]="authService.isCustomer() ? 8 : 7" style="text-align: center; color: var(--gray-600); padding: 2.5rem;">
                    No se encontraron transacciones registradas.
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- Indicador de carga al hacer scroll -->
            <div *ngIf="loadingMore" class="scroll-footer loading-state">
              <span class="pulse-dot">●</span> Cargando siguientes registros...
            </div>

            <!-- Indicador de fin de historial -->
            <div *ngIf="isLastPage && transactions.length > 0" class="scroll-footer end-state">
              ✓ Se cargaron todas las {{ transactions.length }} transacciones disponibles
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .tabs {
      display: flex;
      gap: 0.5rem;
      border-bottom: 2px solid var(--gray-200);
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
    }
    .tab-btn {
      background: none;
      border: none;
      padding: 0.75rem 1.25rem;
      font-size: 0.9rem;
      font-weight: 600;
      color: var(--gray-600);
      cursor: pointer;
      border-bottom: 2px solid transparent;
      margin-bottom: -2px;
      transition: all 0.2s;
    }
    .tab-btn:hover {
      color: var(--primary);
    }
    .tab-btn.active {
      color: var(--primary);
      border-bottom-color: var(--primary);
    }
    .tab-content {
      animation: fadeIn 0.2s ease-in-out;
    }

    /* Comprobante / Voucher Card */
    .voucher-box {
      background: #f0fdf4;
      border: 1px solid #bbf7d0;
      border-radius: var(--radius);
      padding: 1rem 1.25rem;
      margin-bottom: 1.5rem;
      animation: fadeIn 0.25s ease-in-out;
    }
    .voucher-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.75rem;
      padding-bottom: 0.5rem;
      border-bottom: 1px dashed #86efac;
    }
    .voucher-title {
      font-weight: 700;
      font-size: 0.95rem;
      color: #166534;
      display: flex;
      align-items: center;
      gap: 0.4rem;
    }
    .voucher-icon {
      background: #22c55e;
      color: white;
      border-radius: 50%;
      width: 18px;
      height: 18px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 0.75rem;
    }
    .voucher-close {
      background: none;
      border: none;
      font-size: 1.1rem;
      color: #15803d;
      cursor: pointer;
      line-height: 1;
      padding: 0.2rem 0.4rem;
      border-radius: 4px;
    }
    .voucher-close:hover {
      background: #dcfce7;
    }
    .voucher-body {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 0.75rem;
    }
    .voucher-item {
      display: flex;
      flex-direction: column;
      gap: 0.2rem;
    }
    .voucher-label {
      font-size: 0.75rem;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #15803d;
      font-weight: 600;
    }
    .voucher-value {
      font-size: 0.95rem;
      color: #0f172a;
    }
    .font-highlight {
      font-weight: 700;
      color: #166534;
    }

    /* Historial con scroll infinito */
    .filter-box-client {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1rem;
      padding: 0.75rem 1rem;
      background: var(--gray-50);
      border: 1px solid var(--gray-200);
      border-radius: var(--radius);
      flex-wrap: wrap;
    }
    .history-meta {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.75rem;
      font-size: 0.825rem;
      color: var(--gray-600);
      flex-wrap: wrap;
      gap: 0.5rem;
    }
    .history-scroll-container {
      max-height: 440px;
      overflow-y: auto;
      border: 1px solid var(--gray-200);
      border-radius: var(--radius);
      background: white;
      position: relative;
      box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.02);
    }
    .sticky-th {
      position: sticky;
      top: 0;
      background: var(--gray-100);
      z-index: 2;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
    }
    .movement-pill {
      font-size: 0.725rem;
      font-weight: 600;
      padding: 0.15rem 0.5rem;
      border-radius: 9999px;
      display: inline-block;
      white-space: nowrap;
    }
    .badge-credit {
      background: #dcfce7;
      color: #166534;
    }
    .badge-debit {
      background: #fee2e2;
      color: #991b1b;
    }
    .badge-neutral {
      background: var(--gray-100);
      color: var(--gray-700);
    }
    .scroll-footer {
      text-align: center;
      padding: 0.85rem;
      font-size: 0.825rem;
      border-top: 1px solid var(--gray-200);
    }
    .loading-state {
      background: var(--primary-light);
      color: var(--primary);
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
    }
    .pulse-dot {
      animation: pulse 1s infinite;
    }
    @keyframes pulse {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.3; transform: scale(0.85); }
    }
    .end-state {
      background: var(--gray-50);
      color: var(--gray-600);
      font-weight: 500;
    }
    .spinner-line {
      display: inline-block;
      font-weight: 500;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(4px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class TransactionOperationsComponent implements OnInit {
  activeTab: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER' | 'HISTORY' = 'HISTORY';

  accounts: Account[] = [];
  activeAccounts: Account[] = [];
  myAccounts: Account[] = [];
  myActiveAccounts: Account[] = [];

  // Paginación y Scroll Infinito
  transactions: Transaction[] = [];
  currentPage = 0;
  readonly pageSize = 8;
  totalPages = 0;
  totalElements = 0;
  isLastPage = false;
  loadingInitial = false;
  loadingMore = false;
  filterAccountId = '';

  submitting = false;
  lastTransaction: Transaction | null = null;

  depositForm = {
    accountId: '',
    amount: 100000,
    currency: 'COP' as CurrencyCode,
    description: 'Depósito en ventanilla'
  };

  withdrawalForm = {
    accountId: '',
    amount: 50000,
    currency: 'COP' as CurrencyCode,
    description: 'Retiro en cajero'
  };

  transferForm = {
    sourceAccountId: '',
    destinationAccountId: '',
    amount: 25000,
    currency: 'COP' as CurrencyCode,
    description: 'Transferencia entre cuentas'
  };

  constructor(
    private transactionService: TransactionService,
    private accountService: AccountService,
    public authService: AuthService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.activeTab = this.resolveInitialTab();
    this.loadAccounts();
    this.loadTransactions(true);
  }

  private resolveInitialTab(): 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER' | 'HISTORY' {
    // Si es Auditor o no posee permisos para registrar transacciones de caja, abrir Historial Global
    if (
      this.authService.hasRole('AUDITOR') ||
      (!this.authService.hasPermission('TRANSACTION_DEPOSIT') &&
        !this.authService.hasPermission('TRANSACTION_WITHDRAW') &&
        !this.authService.hasPermission('TRANSACTION_TRANSFER'))
    ) {
      return 'HISTORY';
    }

    if (this.authService.isCustomer()) {
      return this.authService.hasPermission('TRANSACTION_TRANSFER') ? 'TRANSFER' : 'HISTORY';
    }

    if (this.authService.hasPermission('TRANSACTION_DEPOSIT')) {
      return 'DEPOSIT';
    }

    if (this.authService.hasPermission('TRANSACTION_WITHDRAW')) {
      return 'WITHDRAWAL';
    }

    if (this.authService.hasPermission('TRANSACTION_TRANSFER')) {
      return 'TRANSFER';
    }

    return 'HISTORY';
  }

  setTab(tab: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER' | 'HISTORY'): void {
    this.activeTab = tab;
    this.lastTransaction = null;
    if (tab === 'HISTORY') {
      this.loadTransactions(true);
    }
  }

  loadAccounts(): void {
    this.accountService.getAll(undefined, undefined, 0, 100).subscribe({
      next: (res) => {
        this.accounts = res.content;

        if (this.authService.isCustomer()) {
          const custId = this.authService.customerId();
          this.myAccounts = res.content.filter((a) => a.customerId === custId);
          this.myActiveAccounts = this.myAccounts.filter((a) => a.status === 'ACTIVE');
          this.activeAccounts = this.myActiveAccounts;
        } else {
          this.myAccounts = res.content;
          this.myActiveAccounts = res.content.filter((a) => a.status === 'ACTIVE');
          this.activeAccounts = this.myActiveAccounts;
        }

        if (this.myActiveAccounts.length > 0) {
          if (!this.depositForm.accountId) {
            this.depositForm.accountId = this.myActiveAccounts[0].id;
            this.depositForm.currency = this.myActiveAccounts[0].currency;
          }
          if (!this.withdrawalForm.accountId) {
            this.withdrawalForm.accountId = this.myActiveAccounts[0].id;
            this.withdrawalForm.currency = this.myActiveAccounts[0].currency;
          }
          if (!this.transferForm.sourceAccountId) {
            this.transferForm.sourceAccountId = this.myActiveAccounts[0].id;
            this.transferForm.currency = this.myActiveAccounts[0].currency;
          }
          const destination = this.getEligibleDestinationAccounts()[0];
          if (destination) {
            this.transferForm.destinationAccountId = destination.id;
          }
        }
      },
      error: (err) => {
        this.toastService.error('Error al cargar cuentas: ' + (err.error?.detail || err.message));
      }
    });
  }

  loadTransactions(reset = false): void {
    if (reset) {
      this.currentPage = 0;
      this.transactions = [];
      this.isLastPage = false;
      this.loadingInitial = true;
    }

    const filters: { accountId?: string } = {};
    if (this.filterAccountId) {
      filters.accountId = this.filterAccountId;
    }

    this.transactionService.search(filters, this.currentPage, this.pageSize).subscribe({
      next: (res) => {
        this.loadingInitial = false;
        this.loadingMore = false;

        let content = res.content;

        // Si es cliente y no especificó cuenta, filtrar para que solo vea las de sus cuentas
        if (this.authService.isCustomer() && !this.filterAccountId) {
          const myIds = new Set(this.myAccounts.map((a) => a.id));
          content = content.filter(
            (t) => (t.sourceAccountId && myIds.has(t.sourceAccountId)) ||
                   (t.destinationAccountId && myIds.has(t.destinationAccountId))
          );
        }

        this.totalElements = this.authService.isCustomer() && !this.filterAccountId
          ? (this.transactions.length + content.length)
          : res.totalElements;
        this.totalPages = res.totalPages;

        if (reset) {
          this.transactions = content;
        } else {
          const existingIds = new Set(this.transactions.map((t) => t.id));
          const newEntries = content.filter((t) => !existingIds.has(t.id));
          this.transactions = [...this.transactions, ...newEntries];
        }

        this.isLastPage = res.last || (this.currentPage + 1 >= res.totalPages) || (res.content.length < this.pageSize);
      },
      error: (err) => {
        this.loadingInitial = false;
        this.loadingMore = false;
        this.toastService.error('Error al consultar historial: ' + (err.error?.detail || err.message));
      }
    });
  }

  onHistoryScroll(event: Event): void {
    const el = event.target as HTMLElement;
    if (!el) return;

    const reachedBottom = el.scrollHeight - el.scrollTop <= el.clientHeight + 40;
    if (reachedBottom && !this.loadingMore && !this.loadingInitial && !this.isLastPage) {
      this.loadMoreTransactions();
    }
  }

  loadMoreTransactions(): void {
    if (this.loadingMore || this.isLastPage) return;
    this.loadingMore = true;
    this.currentPage++;
    this.loadTransactions(false);
  }

  onDepositAccountChange(): void {
    const acc = this.activeAccounts.find((a) => a.id === this.depositForm.accountId);
    if (acc) this.depositForm.currency = acc.currency;
  }

  onWithdrawAccountChange(): void {
    const acc = this.activeAccounts.find((a) => a.id === this.withdrawalForm.accountId);
    if (acc) this.withdrawalForm.currency = acc.currency;
  }

  onTransferSourceChange(): void {
    const src = this.myActiveAccounts.find((a) => a.id === this.transferForm.sourceAccountId);
    if (src) {
      this.transferForm.currency = src.currency;
      const eligible = this.getEligibleDestinationAccounts();
      if (!eligible.some((a) => a.id === this.transferForm.destinationAccountId)) {
        this.transferForm.destinationAccountId = eligible[0]?.id || '';
      }
    }
  }

  getEligibleDestinationAccounts(): Account[] {
    return this.accounts.filter(
      (a) => a.id !== this.transferForm.sourceAccountId &&
             a.currency === this.transferForm.currency &&
             a.status === 'ACTIVE'
    );
  }

  isMyAccount(accountId?: string): boolean {
    if (!accountId) return false;
    return this.myAccounts.some((a) => a.id === accountId);
  }

  getTransactionDirectionLabel(tx: Transaction): string {
    if (tx.type === 'DEPOSIT') return '📥 Depósito';
    if (tx.type === 'WITHDRAWAL') return '📤 Retiro';
    if (tx.type === 'TRANSFER') {
      const isSrcMine = this.isMyAccount(tx.sourceAccountId);
      const isDstMine = this.isMyAccount(tx.destinationAccountId);
      if (isSrcMine && isDstMine) return '🔄 Entre mis cuentas';
      if (isSrcMine) return '📤 Enviada';
      if (isDstMine) return '📥 Recibida';
      return '🔄 Transferencia';
    }
    return tx.type;
  }

  getTransactionDirectionClass(tx: Transaction): string {
    if (tx.type === 'DEPOSIT') return 'badge-credit';
    if (tx.type === 'WITHDRAWAL') return 'badge-debit';
    if (tx.type === 'TRANSFER') {
      if (this.isMyAccount(tx.sourceAccountId) && !this.isMyAccount(tx.destinationAccountId)) {
        return 'badge-debit';
      }
      if (this.isMyAccount(tx.destinationAccountId)) {
        return 'badge-credit';
      }
    }
    return 'badge-neutral';
  }

  submitDeposit(): void {
    if (!this.depositForm.accountId) {
      this.toastService.warning('Seleccione una cuenta de destino.', 'Validación');
      return;
    }
    if (this.depositForm.amount <= 0) {
      this.toastService.warning('El monto a depositar debe ser mayor a 0.', 'Validación');
      return;
    }

    this.submitting = true;

    this.transactionService.deposit({
      accountId: this.depositForm.accountId,
      amount: this.depositForm.amount,
      currency: this.depositForm.currency,
      description: this.depositForm.description
    }).subscribe({
      next: (tx) => {
        this.submitting = false;
        this.lastTransaction = tx;
        this.toastService.success(
          `Depósito de ${tx.amount} ${tx.currency} aplicado correctamente (Ref: ${tx.reference}).`,
          'Depósito Exitoso'
        );
        this.loadAccounts();
      },
      error: (err) => {
        this.submitting = false;
        const msg = err.error?.detail || err.error?.title || 'Error al procesar depósito.';
        this.toastService.error(msg, 'Depósito Rechazado');
      }
    });
  }

  submitWithdrawal(): void {
    if (!this.withdrawalForm.accountId) {
      this.toastService.warning('Seleccione una cuenta de origen.', 'Validación');
      return;
    }
    if (this.withdrawalForm.amount <= 0) {
      this.toastService.warning('El monto a retirar debe ser mayor a 0.', 'Validación');
      return;
    }

    const acc = this.activeAccounts.find((a) => a.id === this.withdrawalForm.accountId);

    this.toastService.confirm(
      `¿Confirmas el débito y retiro de ${this.withdrawalForm.amount} ${this.withdrawalForm.currency} de la cuenta Nº ${acc?.accountNumber || ''}?`,
      () => this.executeWithdrawal(),
      {
        title: 'Confirmar Retiro Bancario',
        confirmText: 'Retirar Fondos',
        cancelText: 'Cancelar',
        danger: true
      }
    );
  }

  private executeWithdrawal(): void {
    this.submitting = true;

    this.transactionService.withdraw({
      accountId: this.withdrawalForm.accountId,
      amount: this.withdrawalForm.amount,
      currency: this.withdrawalForm.currency,
      description: this.withdrawalForm.description
    }).subscribe({
      next: (tx) => {
        this.submitting = false;
        this.lastTransaction = tx;
        this.toastService.success(
          `Retiro de ${tx.amount} ${tx.currency} aplicado correctamente (Ref: ${tx.reference}).`,
          'Retiro Exitoso'
        );
        this.loadAccounts();
      },
      error: (err) => {
        this.submitting = false;
        const msg = err.error?.detail || err.error?.title || 'Error al procesar retiro.';
        this.toastService.error(msg, 'Retiro Rechazado');
      }
    });
  }

  submitTransfer(): void {
    if (!this.transferForm.sourceAccountId || !this.transferForm.destinationAccountId) {
      this.toastService.warning('Debe seleccionar cuenta de origen y de destino.', 'Validación');
      return;
    }
    if (this.transferForm.sourceAccountId === this.transferForm.destinationAccountId) {
      this.toastService.warning('La cuenta origen y la cuenta destino deben ser diferentes.', 'Validación');
      return;
    }
    if (this.transferForm.amount <= 0) {
      this.toastService.warning('El monto a transferir debe ser mayor a 0.', 'Validación');
      return;
    }

    const src = this.myActiveAccounts.find((a) => a.id === this.transferForm.sourceAccountId);
    const dst = this.accounts.find((a) => a.id === this.transferForm.destinationAccountId);

    this.toastService.confirm(
      `¿Deseas ejecutar la transferencia de ${this.transferForm.amount} ${this.transferForm.currency} desde la cuenta Nº ${src?.accountNumber || ''} a la cuenta Nº ${dst?.accountNumber || ''}?`,
      () => this.executeTransfer(),
      {
        title: 'Confirmar Transferencia Atómica',
        confirmText: 'Transferir Fondos',
        cancelText: 'Cancelar'
      }
    );
  }

  private executeTransfer(): void {
    this.submitting = true;

    this.transactionService.transfer({
      sourceAccountId: this.transferForm.sourceAccountId,
      destinationAccountId: this.transferForm.destinationAccountId,
      amount: this.transferForm.amount,
      currency: this.transferForm.currency,
      description: this.transferForm.description
    }).subscribe({
      next: (tx) => {
        this.submitting = false;
        this.lastTransaction = tx;
        this.toastService.success(
          `Transferencia de ${tx.amount} ${tx.currency} completada con éxito (Ref: ${tx.reference}).`,
          'Transferencia Exitosa'
        );
        this.loadAccounts();
      },
      error: (err) => {
        this.submitting = false;
        const msg = err.error?.detail || err.error?.title || 'Error al procesar transferencia.';
        this.toastService.error(msg, 'Transferencia Rechazada');
      }
    });
  }
}
