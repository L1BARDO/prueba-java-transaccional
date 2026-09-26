import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransactionService } from '../../core/services/transaction.service';
import { AccountService } from '../../core/services/account.service';
import { CustomerService } from '../../core/services/customer.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Account, CurrencyCode, Customer, Transaction } from '../../core/models/models';

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
              <label class="form-label" style="display: flex; justify-content: space-between; align-items: center;">
                <span>Cuenta Destino</span>
                <small style="color: var(--gray-600); font-weight: normal;">Buscar por nombre de titular, cédula o número de cuenta</small>
              </label>

              <div class="searchable-select-container">
                <div class="dropdown-backdrop" *ngIf="depositDropdownOpen" (click)="depositDropdownOpen = false"></div>

                <div class="search-input-wrapper">
                  <span class="search-icon">🔍</span>
                  <input
                    type="text"
                    class="form-control search-input"
                    placeholder="Escriba nombre, cédula o cuenta para filtrar..."
                    [(ngModel)]="depositSearch"
                    (focus)="depositDropdownOpen = true"
                    name="depSearch"
                    autocomplete="off"
                  />
                  <button
                    *ngIf="depositSearch || selectedDepositAccount"
                    type="button"
                    class="btn-clear-search"
                    (click)="clearDepositSearch()"
                    title="Limpiar / Cambiar"
                  >
                    ✕
                  </button>
                </div>

                <!-- Tarjeta de cuenta seleccionada -->
                <div *ngIf="selectedDepositAccount" class="selected-account-card">
                  <div class="card-left">
                    <div class="card-acc-row">
                      <span class="acc-title">Cuenta {{ selectedDepositAccount.accountNumber }}</span>
                      <span class="badge-type">{{ selectedDepositAccount.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}</span>
                      <span class="badge-currency">{{ selectedDepositAccount.currency }}</span>
                    </div>
                    <div class="card-client-row" *ngIf="selectedDepositAccount.customerName || getCustomerName(selectedDepositAccount.customerId)">
                      <span class="client-name">👤 {{ selectedDepositAccount.customerName || getCustomerName(selectedDepositAccount.customerId) }}</span>
                      <span class="client-doc" *ngIf="selectedDepositAccount.customerDocumentNumber || getCustomerDoc(selectedDepositAccount.customerId)">
                        · Doc: {{ selectedDepositAccount.customerDocumentNumber || getCustomerDoc(selectedDepositAccount.customerId) }}
                      </span>
                    </div>
                  </div>
                  <div class="card-right">
                    <span class="balance-lbl">Saldo actual</span>
                    <span class="balance-val">{{ selectedDepositAccount.balance | currency: selectedDepositAccount.currency : 'symbol' : '1.2-2' }}</span>
                  </div>
                </div>

                <!-- Menú desplegable -->
                <div class="search-dropdown-menu" *ngIf="depositDropdownOpen">
                  <div class="dropdown-header">
                    <span>Cuentas disponibles ({{ getFilteredDepositAccounts().length }})</span>
                    <button type="button" class="btn-close-dropdown" (click)="depositDropdownOpen = false">Cerrar ✕</button>
                  </div>
                  <div class="dropdown-list">
                    <div
                      *ngFor="let a of getFilteredDepositAccounts()"
                      class="dropdown-item"
                      [class.selected]="selectedDepositAccount?.id === a.id"
                      (click)="selectDepositAccount(a)"
                    >
                      <div class="item-left">
                        <div class="item-acc">
                          <strong>Cuenta {{ a.accountNumber }}</strong>
                          <span class="item-badge">{{ a.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}</span>
                          <span class="item-currency">{{ a.currency }}</span>
                        </div>
                        <div class="item-customer" *ngIf="a.customerName || getCustomerName(a.customerId)">
                          <span>👤 {{ a.customerName || getCustomerName(a.customerId) }}</span>
                          <span class="item-doc" *ngIf="a.customerDocumentNumber || getCustomerDoc(a.customerId)">
                            · Doc: {{ a.customerDocumentNumber || getCustomerDoc(a.customerId) }}
                          </span>
                        </div>
                      </div>
                      <div class="item-right">
                        <span class="item-balance">{{ a.balance | currency: a.currency : 'symbol' : '1.2-2' }}</span>
                      </div>
                    </div>
                    <div *ngIf="getFilteredDepositAccounts().length === 0" class="dropdown-empty">
                      No se encontraron cuentas con "{{ depositSearch }}".
                    </div>
                  </div>
                </div>
              </div>
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
              <label class="form-label" style="display: flex; justify-content: space-between; align-items: center;">
                <span>Cuenta Origen para Retiro</span>
                <small style="color: var(--gray-600); font-weight: normal;">Buscar por nombre de titular, cédula o número de cuenta</small>
              </label>

              <div class="searchable-select-container">
                <div class="dropdown-backdrop" *ngIf="withdrawDropdownOpen" (click)="withdrawDropdownOpen = false"></div>

                <div class="search-input-wrapper">
                  <span class="search-icon">🔍</span>
                  <input
                    type="text"
                    class="form-control search-input"
                    placeholder="Escriba nombre, cédula o cuenta para filtrar..."
                    [(ngModel)]="withdrawSearch"
                    (focus)="withdrawDropdownOpen = true"
                    name="wSearch"
                    autocomplete="off"
                  />
                  <button
                    *ngIf="withdrawSearch || selectedWithdrawAccount"
                    type="button"
                    class="btn-clear-search"
                    (click)="clearWithdrawSearch()"
                    title="Limpiar / Cambiar"
                  >
                    ✕
                  </button>
                </div>

                <!-- Tarjeta de cuenta seleccionada -->
                <div *ngIf="selectedWithdrawAccount" class="selected-account-card">
                  <div class="card-left">
                    <div class="card-acc-row">
                      <span class="acc-title">Cuenta {{ selectedWithdrawAccount.accountNumber }}</span>
                      <span class="badge-type">{{ selectedWithdrawAccount.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}</span>
                      <span class="badge-currency">{{ selectedWithdrawAccount.currency }}</span>
                    </div>
                    <div class="card-client-row" *ngIf="selectedWithdrawAccount.customerName || getCustomerName(selectedWithdrawAccount.customerId)">
                      <span class="client-name">👤 {{ selectedWithdrawAccount.customerName || getCustomerName(selectedWithdrawAccount.customerId) }}</span>
                      <span class="client-doc" *ngIf="selectedWithdrawAccount.customerDocumentNumber || getCustomerDoc(selectedWithdrawAccount.customerId)">
                        · Doc: {{ selectedWithdrawAccount.customerDocumentNumber || getCustomerDoc(selectedWithdrawAccount.customerId) }}
                      </span>
                    </div>
                  </div>
                  <div class="card-right">
                    <span class="balance-lbl">Saldo disponible</span>
                    <span class="balance-val">{{ selectedWithdrawAccount.balance | currency: selectedWithdrawAccount.currency : 'symbol' : '1.2-2' }}</span>
                  </div>
                </div>

                <!-- Menú desplegable -->
                <div class="search-dropdown-menu" *ngIf="withdrawDropdownOpen">
                  <div class="dropdown-header">
                    <span>Cuentas disponibles ({{ getFilteredWithdrawAccounts().length }})</span>
                    <button type="button" class="btn-close-dropdown" (click)="withdrawDropdownOpen = false">Cerrar ✕</button>
                  </div>
                  <div class="dropdown-list">
                    <div
                      *ngFor="let a of getFilteredWithdrawAccounts()"
                      class="dropdown-item"
                      [class.selected]="selectedWithdrawAccount?.id === a.id"
                      (click)="selectWithdrawAccount(a)"
                    >
                      <div class="item-left">
                        <div class="item-acc">
                          <strong>Cuenta {{ a.accountNumber }}</strong>
                          <span class="item-badge">{{ a.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}</span>
                          <span class="item-currency">{{ a.currency }}</span>
                        </div>
                        <div class="item-customer" *ngIf="a.customerName || getCustomerName(a.customerId)">
                          <span>👤 {{ a.customerName || getCustomerName(a.customerId) }}</span>
                          <span class="item-doc" *ngIf="a.customerDocumentNumber || getCustomerDoc(a.customerId)">
                            · Doc: {{ a.customerDocumentNumber || getCustomerDoc(a.customerId) }}
                          </span>
                        </div>
                      </div>
                      <div class="item-right">
                        <span class="item-balance">{{ a.balance | currency: a.currency : 'symbol' : '1.2-2' }}</span>
                      </div>
                    </div>
                    <div *ngIf="getFilteredWithdrawAccounts().length === 0" class="dropdown-empty">
                      No se encontraron cuentas con "{{ withdrawSearch }}".
                    </div>
                  </div>
                </div>
              </div>
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
              <!-- Cuenta Origen (Débito) -->
              <div class="form-group">
                <label class="form-label" style="display: flex; justify-content: space-between; align-items: center;">
                  <span>Cuenta Origen (Débito)</span>
                  <small style="color: var(--gray-600); font-weight: normal;">Buscar por titular, cédula o cuenta</small>
                </label>

                <div class="searchable-select-container">
                  <div class="dropdown-backdrop" *ngIf="transferSourceDropdownOpen" (click)="transferSourceDropdownOpen = false"></div>

                  <div class="search-input-wrapper">
                    <span class="search-icon">🔍</span>
                    <input
                      type="text"
                      class="form-control search-input"
                      placeholder="Buscar cuenta origen por nombre, cédula o cuenta..."
                      [(ngModel)]="transferSourceSearch"
                      (focus)="transferSourceDropdownOpen = true"
                      name="trSourceSearch"
                      autocomplete="off"
                    />
                    <button
                      *ngIf="transferSourceSearch || selectedTransferSourceAccount"
                      type="button"
                      class="btn-clear-search"
                      (click)="clearTransferSourceSearch()"
                      title="Limpiar / Cambiar"
                    >
                      ✕
                    </button>
                  </div>

                  <!-- Tarjeta visual cuenta origen -->
                  <div *ngIf="selectedTransferSourceAccount" class="selected-account-card">
                    <div class="card-left">
                      <div class="card-acc-row">
                        <span class="acc-title">Cuenta {{ selectedTransferSourceAccount.accountNumber }}</span>
                        <span class="badge-type">{{ selectedTransferSourceAccount.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}</span>
                        <span class="badge-currency">{{ selectedTransferSourceAccount.currency }}</span>
                      </div>
                      <div class="card-client-row" *ngIf="selectedTransferSourceAccount.customerName || getCustomerName(selectedTransferSourceAccount.customerId)">
                        <span class="client-name">👤 {{ selectedTransferSourceAccount.customerName || getCustomerName(selectedTransferSourceAccount.customerId) }}</span>
                        <span class="client-doc" *ngIf="selectedTransferSourceAccount.customerDocumentNumber || getCustomerDoc(selectedTransferSourceAccount.customerId)">
                          · Doc: {{ selectedTransferSourceAccount.customerDocumentNumber || getCustomerDoc(selectedTransferSourceAccount.customerId) }}
                        </span>
                      </div>
                    </div>
                    <div class="card-right">
                      <span class="balance-lbl">Saldo disponible</span>
                      <span class="balance-val">{{ selectedTransferSourceAccount.balance | currency: selectedTransferSourceAccount.currency : 'symbol' : '1.2-2' }}</span>
                    </div>
                  </div>

                  <!-- Menú desplegable cuenta origen -->
                  <div class="search-dropdown-menu" *ngIf="transferSourceDropdownOpen">
                    <div class="dropdown-header">
                      <span>Cuentas de origen ({{ getFilteredTransferSourceAccounts().length }})</span>
                      <button type="button" class="btn-close-dropdown" (click)="transferSourceDropdownOpen = false">Cerrar ✕</button>
                    </div>
                    <div class="dropdown-list">
                      <div
                        *ngFor="let a of getFilteredTransferSourceAccounts()"
                        class="dropdown-item"
                        [class.selected]="selectedTransferSourceAccount?.id === a.id"
                        (click)="selectTransferSourceAccount(a)"
                      >
                        <div class="item-left">
                          <div class="item-acc">
                            <strong>Cuenta {{ a.accountNumber }}</strong>
                            <span class="item-badge">{{ a.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}</span>
                            <span class="item-currency">{{ a.currency }}</span>
                          </div>
                          <div class="item-customer" *ngIf="a.customerName || getCustomerName(a.customerId)">
                            <span>👤 {{ a.customerName || getCustomerName(a.customerId) }}</span>
                            <span class="item-doc" *ngIf="a.customerDocumentNumber || getCustomerDoc(a.customerId)">
                              · Doc: {{ a.customerDocumentNumber || getCustomerDoc(a.customerId) }}
                            </span>
                          </div>
                        </div>
                        <div class="item-right">
                          <span class="item-balance">{{ a.balance | currency: a.currency : 'symbol' : '1.2-2' }}</span>
                        </div>
                      </div>
                      <div *ngIf="getFilteredTransferSourceAccounts().length === 0" class="dropdown-empty">
                        No se encontraron cuentas con "{{ transferSourceSearch }}".
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Cuenta Destino (Crédito) -->
              <div class="form-group">
                <label class="form-label" style="display: flex; justify-content: space-between; align-items: center;">
                  <span>Cuenta Destino (Crédito)</span>
                  <small style="color: var(--gray-600); font-weight: normal;">Buscar destinatario por nombre o cédula</small>
                </label>

                <div class="searchable-select-container">
                  <div class="dropdown-backdrop" *ngIf="transferDestDropdownOpen" (click)="transferDestDropdownOpen = false"></div>

                  <div class="search-input-wrapper">
                    <span class="search-icon">🔍</span>
                    <input
                      type="text"
                      class="form-control search-input"
                      placeholder="Buscar destinatario por nombre, cédula o cuenta..."
                      [(ngModel)]="transferDestSearch"
                      (focus)="transferDestDropdownOpen = true"
                      name="trDestSearch"
                      autocomplete="off"
                    />
                    <button
                      *ngIf="transferDestSearch || selectedTransferDestAccount"
                      type="button"
                      class="btn-clear-search"
                      (click)="clearTransferDestSearch()"
                      title="Limpiar / Cambiar"
                    >
                      ✕
                    </button>
                  </div>

                  <!-- Tarjeta visual cuenta destino -->
                  <div *ngIf="selectedTransferDestAccount" class="selected-account-card">
                    <div class="card-left">
                      <div class="card-acc-row">
                        <span class="acc-title">Cuenta {{ selectedTransferDestAccount.accountNumber }}</span>
                        <span class="badge-type">{{ selectedTransferDestAccount.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}</span>
                        <span class="badge-currency">{{ selectedTransferDestAccount.currency }}</span>
                      </div>
                      <div class="card-client-row" *ngIf="selectedTransferDestAccount.customerName || getCustomerName(selectedTransferDestAccount.customerId)">
                        <span class="client-name">👤 {{ selectedTransferDestAccount.customerName || getCustomerName(selectedTransferDestAccount.customerId) }}</span>
                        <span class="client-doc" *ngIf="selectedTransferDestAccount.customerDocumentNumber || getCustomerDoc(selectedTransferDestAccount.customerId)">
                          · Doc: {{ selectedTransferDestAccount.customerDocumentNumber || getCustomerDoc(selectedTransferDestAccount.customerId) }}
                        </span>
                      </div>
                    </div>
                    <div class="card-right">
                      <span class="badge badge-active">Lista para recibir</span>
                    </div>
                  </div>

                  <!-- Menú desplegable cuenta destino -->
                  <div class="search-dropdown-menu" *ngIf="transferDestDropdownOpen">
                    <div class="dropdown-header">
                      <span>Cuentas elegibles ({{ getFilteredTransferDestAccounts().length }})</span>
                      <button type="button" class="btn-close-dropdown" (click)="transferDestDropdownOpen = false">Cerrar ✕</button>
                    </div>
                    <div class="dropdown-list">
                      <div
                        *ngFor="let a of getFilteredTransferDestAccounts()"
                        class="dropdown-item"
                        [class.selected]="selectedTransferDestAccount?.id === a.id"
                        (click)="selectTransferDestAccount(a)"
                      >
                        <div class="item-left">
                          <div class="item-acc">
                            <strong>Cuenta {{ a.accountNumber }}</strong>
                            <span class="item-badge">{{ a.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}</span>
                            <span class="item-currency">{{ a.currency }}</span>
                          </div>
                          <div class="item-customer" *ngIf="a.customerName || getCustomerName(a.customerId)">
                            <span>👤 {{ a.customerName || getCustomerName(a.customerId) }}</span>
                            <span class="item-doc" *ngIf="a.customerDocumentNumber || getCustomerDoc(a.customerId)">
                              · Doc: {{ a.customerDocumentNumber || getCustomerDoc(a.customerId) }}
                            </span>
                          </div>
                        </div>
                        <div class="item-right">
                          <span class="item-badge" style="background: #ecfdf5; color: #047857;">Misma Moneda ({{ a.currency }})</span>
                        </div>
                      </div>
                      <div *ngIf="getFilteredTransferDestAccounts().length === 0" class="dropdown-empty">
                        No se encontraron cuentas destino con "{{ transferDestSearch }}".
                      </div>
                    </div>
                  </div>
                </div>
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

    /* Searchable Select & Cards */
    .searchable-select-container {
      position: relative;
      margin-bottom: 0.5rem;
    }
    .dropdown-backdrop {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      z-index: 40;
    }
    .search-input-wrapper {
      position: relative;
      display: flex;
      align-items: center;
    }
    .search-icon {
      position: absolute;
      left: 0.75rem;
      font-size: 0.85rem;
      pointer-events: none;
      color: var(--gray-600);
    }
    .search-input {
      padding-left: 2.25rem !important;
      padding-right: 2rem !important;
      border-radius: var(--radius);
      border: 1px solid var(--gray-300);
      font-size: 0.875rem;
      width: 100%;
    }
    .search-input:focus {
      border-color: var(--primary);
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.15);
      outline: none;
    }
    .btn-clear-search {
      position: absolute;
      right: 0.6rem;
      background: none;
      border: none;
      color: var(--gray-600);
      cursor: pointer;
      font-size: 0.85rem;
      padding: 0.2rem 0.4rem;
      border-radius: 4px;
    }
    .btn-clear-search:hover {
      background: var(--gray-200);
      color: var(--gray-800);
    }

    /* Selected account summary card */
    .selected-account-card {
      margin-top: 0.5rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: #f8fafc;
      border: 1px solid #cbd5e1;
      border-left: 4px solid var(--primary);
      border-radius: var(--radius);
      padding: 0.65rem 0.85rem;
      gap: 0.75rem;
    }
    .card-left {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }
    .card-acc-row {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      flex-wrap: wrap;
    }
    .acc-title {
      font-weight: 700;
      color: #0f172a;
      font-size: 0.9rem;
    }
    .badge-type {
      background: #e2e8f0;
      color: #334155;
      font-size: 0.7rem;
      font-weight: 600;
      padding: 0.1rem 0.4rem;
      border-radius: 4px;
    }
    .badge-currency {
      background: #dbeafe;
      color: #1e40af;
      font-size: 0.7rem;
      font-weight: 700;
      padding: 0.1rem 0.4rem;
      border-radius: 4px;
    }
    .card-client-row {
      font-size: 0.8rem;
      color: #475569;
      display: flex;
      gap: 0.35rem;
      flex-wrap: wrap;
    }
    .client-name {
      font-weight: 600;
      color: #1e293b;
    }
    .client-doc {
      color: #64748b;
    }
    .card-right {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      min-width: 100px;
    }
    .balance-lbl {
      font-size: 0.7rem;
      color: #64748b;
      text-transform: uppercase;
      font-weight: 600;
    }
    .balance-val {
      font-size: 0.95rem;
      font-weight: 700;
      color: #059669;
    }

    /* Dropdown menu */
    .search-dropdown-menu {
      position: absolute;
      top: 100%;
      left: 0;
      right: 0;
      z-index: 50;
      background: white;
      border: 1px solid var(--gray-300);
      border-radius: var(--radius);
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
      margin-top: 4px;
      overflow: hidden;
      animation: fadeIn 0.15s ease-in-out;
    }
    .dropdown-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.4rem 0.75rem;
      background: var(--gray-100);
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--gray-600);
      border-bottom: 1px solid var(--gray-200);
    }
    .btn-close-dropdown {
      background: none;
      border: none;
      font-size: 0.75rem;
      color: var(--gray-600);
      cursor: pointer;
      font-weight: 600;
    }
    .btn-close-dropdown:hover {
      color: var(--danger);
    }
    .dropdown-list {
      max-height: 240px;
      overflow-y: auto;
    }
    .dropdown-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.6rem 0.75rem;
      border-bottom: 1px solid var(--gray-100);
      cursor: pointer;
      transition: background 0.15s ease;
    }
    .dropdown-item:hover {
      background: #eff6ff;
    }
    .dropdown-item.selected {
      background: #e0f2fe;
      border-left: 3px solid var(--primary);
    }
    .dropdown-item:last-child {
      border-bottom: none;
    }
    .item-left {
      display: flex;
      flex-direction: column;
      gap: 0.15rem;
    }
    .item-acc {
      display: flex;
      align-items: center;
      gap: 0.4rem;
      font-size: 0.85rem;
    }
    .item-badge {
      background: var(--gray-200);
      color: var(--gray-700);
      font-size: 0.65rem;
      padding: 0.1rem 0.35rem;
      border-radius: 3px;
      font-weight: 600;
    }
    .item-currency {
      background: #dbeafe;
      color: #1e40af;
      font-size: 0.65rem;
      padding: 0.1rem 0.35rem;
      border-radius: 3px;
      font-weight: 700;
    }
    .item-customer {
      font-size: 0.75rem;
      color: var(--gray-600);
    }
    .item-doc {
      color: var(--gray-500);
    }
    .item-right {
      text-align: right;
    }
    .item-balance {
      font-weight: 700;
      font-size: 0.85rem;
      color: #059669;
    }
    .dropdown-empty {
      padding: 1.25rem;
      text-align: center;
      color: var(--gray-500);
      font-size: 0.85rem;
      font-style: italic;
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

  // Customer resolution cache
  customersMap = new Map<string, Customer>();

  // Search & Selection State for Depósito
  depositSearch = '';
  depositDropdownOpen = false;
  selectedDepositAccount: Account | null = null;

  // Search & Selection State for Retiro
  withdrawSearch = '';
  withdrawDropdownOpen = false;
  selectedWithdrawAccount: Account | null = null;

  // Search & Selection State for Transferencia - Origen
  transferSourceSearch = '';
  transferSourceDropdownOpen = false;
  selectedTransferSourceAccount: Account | null = null;

  // Search & Selection State for Transferencia - Destino
  transferDestSearch = '';
  transferDestDropdownOpen = false;
  selectedTransferDestAccount: Account | null = null;

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
    private customerService: CustomerService,
    public authService: AuthService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.activeTab = this.resolveInitialTab();
    if (this.authService.hasPermission('CUSTOMER_READ')) {
      this.customerService.getAll(0, 100).subscribe({
        next: (res) => {
          res.content.forEach((c) => this.customersMap.set(c.id, c));
        },
        error: () => {}
      });
    }
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

        if (this.activeAccounts.length > 0) {
          if (!this.selectedDepositAccount || !this.activeAccounts.some((a) => a.id === this.selectedDepositAccount?.id)) {
            this.selectDepositAccount(this.activeAccounts[0]);
          } else {
            const updated = this.activeAccounts.find((a) => a.id === this.selectedDepositAccount?.id);
            if (updated) this.selectedDepositAccount = updated;
          }

          if (!this.selectedWithdrawAccount || !this.activeAccounts.some((a) => a.id === this.selectedWithdrawAccount?.id)) {
            this.selectWithdrawAccount(this.activeAccounts[0]);
          } else {
            const updated = this.activeAccounts.find((a) => a.id === this.selectedWithdrawAccount?.id);
            if (updated) this.selectedWithdrawAccount = updated;
          }
        }

        if (this.myActiveAccounts.length > 0) {
          if (!this.selectedTransferSourceAccount || !this.myActiveAccounts.some((a) => a.id === this.selectedTransferSourceAccount?.id)) {
            this.selectTransferSourceAccount(this.myActiveAccounts[0]);
          } else {
            const updated = this.myActiveAccounts.find((a) => a.id === this.selectedTransferSourceAccount?.id);
            if (updated) this.selectedTransferSourceAccount = updated;
          }

          const eligible = this.getEligibleDestinationAccounts();
          if (eligible.length > 0) {
            if (!this.selectedTransferDestAccount || !eligible.some((a) => a.id === this.selectedTransferDestAccount?.id)) {
              this.selectTransferDestAccount(eligible[0]);
            } else {
              const updated = eligible.find((a) => a.id === this.selectedTransferDestAccount?.id);
              if (updated) this.selectedTransferDestAccount = updated;
            }
          } else {
            this.selectTransferDestAccount(null);
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

  getCustomerName(customerId: string): string {
    return this.customersMap.get(customerId)?.fullName || '';
  }

  getCustomerDoc(customerId: string): string {
    return this.customersMap.get(customerId)?.documentNumber || '';
  }

  private matchesFilter(acc: Account, term: string): boolean {
    if (!term || !term.trim()) return true;
    const q = term.trim().toLowerCase();
    const accNum = (acc.accountNumber || '').toLowerCase();
    const custName = (acc.customerName || this.getCustomerName(acc.customerId) || '').toLowerCase();
    const custDoc = (acc.customerDocumentNumber || this.getCustomerDoc(acc.customerId) || '').toLowerCase();
    return accNum.includes(q) || custName.includes(q) || custDoc.includes(q);
  }

  getFilteredDepositAccounts(): Account[] {
    return this.activeAccounts.filter((a) => this.matchesFilter(a, this.depositSearch));
  }

  selectDepositAccount(acc: Account): void {
    this.selectedDepositAccount = acc;
    this.depositForm.accountId = acc.id;
    this.depositForm.currency = acc.currency;
    this.depositDropdownOpen = false;
    this.depositSearch = '';
  }

  clearDepositSearch(): void {
    this.depositSearch = '';
    this.depositDropdownOpen = true;
  }

  getFilteredWithdrawAccounts(): Account[] {
    return this.activeAccounts.filter((a) => this.matchesFilter(a, this.withdrawSearch));
  }

  selectWithdrawAccount(acc: Account): void {
    this.selectedWithdrawAccount = acc;
    this.withdrawalForm.accountId = acc.id;
    this.withdrawalForm.currency = acc.currency;
    this.withdrawDropdownOpen = false;
    this.withdrawSearch = '';
  }

  clearWithdrawSearch(): void {
    this.withdrawSearch = '';
    this.withdrawDropdownOpen = true;
  }

  getFilteredTransferSourceAccounts(): Account[] {
    return this.myActiveAccounts.filter((a) => this.matchesFilter(a, this.transferSourceSearch));
  }

  selectTransferSourceAccount(acc: Account): void {
    this.selectedTransferSourceAccount = acc;
    this.transferForm.sourceAccountId = acc.id;
    this.transferForm.currency = acc.currency;
    this.transferSourceDropdownOpen = false;
    this.transferSourceSearch = '';

    const eligible = this.getEligibleDestinationAccounts();
    if (!this.selectedTransferDestAccount || this.selectedTransferDestAccount.id === acc.id || this.selectedTransferDestAccount.currency !== acc.currency) {
      this.selectTransferDestAccount(eligible[0] || null);
    }
  }

  clearTransferSourceSearch(): void {
    this.transferSourceSearch = '';
    this.transferSourceDropdownOpen = true;
  }

  getFilteredTransferDestAccounts(): Account[] {
    return this.getEligibleDestinationAccounts().filter((a) => this.matchesFilter(a, this.transferDestSearch));
  }

  selectTransferDestAccount(acc: Account | null): void {
    this.selectedTransferDestAccount = acc;
    this.transferForm.destinationAccountId = acc ? acc.id : '';
    this.transferDestDropdownOpen = false;
    this.transferDestSearch = '';
  }

  clearTransferDestSearch(): void {
    this.transferDestSearch = '';
    this.transferDestDropdownOpen = true;
  }

  onDepositAccountChange(): void {
    const acc = this.activeAccounts.find((a) => a.id === this.depositForm.accountId);
    if (acc) this.selectDepositAccount(acc);
  }

  onWithdrawAccountChange(): void {
    const acc = this.activeAccounts.find((a) => a.id === this.withdrawalForm.accountId);
    if (acc) this.selectWithdrawAccount(acc);
  }

  onTransferSourceChange(): void {
    const src = this.myActiveAccounts.find((a) => a.id === this.transferForm.sourceAccountId);
    if (src) this.selectTransferSourceAccount(src);
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
