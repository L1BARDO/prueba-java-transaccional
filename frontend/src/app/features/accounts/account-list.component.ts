import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AccountService } from '../../core/services/account.service';
import { CustomerService } from '../../core/services/customer.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { Account, AccountType, CurrencyCode, Customer, Movement } from '../../core/models/models';

@Component({
  selector: 'app-account-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="container">
      <div class="card">
        <div class="card-header">
          <div>
            <h1 class="card-title">{{ authService.isCustomer() ? 'Mis Cuentas Bancarias' : 'Cuentas Bancarias' }}</h1>
            <p style="font-size: 0.85rem; color: var(--gray-600);">
              {{ authService.isCustomer() ? 'Consulta el saldo disponible y extractos de movimientos de tus cuentas.' : 'Gestión de cuentas de ahorro y corrientes con dueñas de saldo y control de concurrencia.' }}
            </p>
          </div>
          <button
            *ngIf="!authService.isCustomer() && authService.hasPermission('ACCOUNT_CREATE')"
            (click)="openCreateModal()"
            class="btn btn-primary"
          >
            <span>+</span> Apertura de Cuenta
          </button>
        </div>

        <div class="filters-row" style="display: flex; gap: 1rem; margin-bottom: 1rem; align-items: center;">
          <div *ngIf="!authService.isCustomer() && authService.hasPermission('CUSTOMER_READ')" style="flex: 1;">
            <label class="form-label">Filtrar por Cliente:</label>
            <select class="form-control" [(ngModel)]="selectedCustomerId" (change)="loadAccounts()">
              <option value="">-- Todos los Clientes --</option>
              <option *ngFor="let c of customers" [value]="c.id">
                {{ c.fullName }} ({{ c.documentNumber }})
              </option>
            </select>
          </div>

          <div style="flex: 1;">
            <label class="form-label">Filtrar por Estado:</label>
            <select class="form-control" [(ngModel)]="selectedStatus" (change)="loadAccounts()">
              <option value="">-- Todos los Estados --</option>
              <option value="ACTIVE">Activa (ACTIVE)</option>
              <option value="BLOCKED">Bloqueada (BLOCKED)</option>
              <option value="CLOSED">Cerrada (CLOSED)</option>
            </select>
          </div>
        </div>

        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>Número de Cuenta</th>
                <th>Tipo</th>
                <th>Moneda</th>
                <th>Saldo Disponible</th>
                <th>Estado</th>
                <th>Fecha Apertura</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let a of accounts">
                <td><strong>{{ a.accountNumber }}</strong></td>
                <td>{{ a.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }}</td>
                <td><span style="font-weight: 600;">{{ a.currency }}</span></td>
                <td style="font-size: 1rem; font-weight: 700; color: var(--primary);">
                  {{ a.balance | currency: a.currency : 'symbol' : '1.2-2' }}
                </td>
                <td>
                  <span
                    class="badge"
                    [ngClass]="{
                      'badge-active': a.status === 'ACTIVE',
                      'badge-blocked': a.status === 'BLOCKED',
                      'badge-closed': a.status === 'CLOSED'
                    }"
                  >
                    {{ a.status }}
                  </span>
                </td>
                <td>{{ a.createdAt | date:'short' }}</td>
                <td>
                  <div style="display: flex; gap: 0.35rem;">
                    <button class="btn btn-outline btn-sm" (click)="viewStatement(a)">
                      📄 Extracto
                    </button>
                    <a
                      *ngIf="authService.isCustomer() && a.status === 'ACTIVE'"
                      routerLink="/transactions"
                      class="btn btn-primary btn-sm"
                    >
                      🔄 Transferir
                    </a>
                    <ng-container *ngIf="!authService.isCustomer()">
                      <button
                        *ngIf="a.status === 'ACTIVE' && authService.hasPermission('ACCOUNT_UPDATE_STATUS')"
                        class="btn btn-warning btn-sm"
                        (click)="toggleStatus(a, 'BLOCKED')"
                      >
                        Bloquear
                      </button>
                      <button
                        *ngIf="a.status === 'BLOCKED' && authService.hasPermission('ACCOUNT_UPDATE_STATUS')"
                        class="btn btn-success btn-sm"
                        (click)="toggleStatus(a, 'ACTIVE')"
                      >
                        Activar
                      </button>
                      <button
                        *ngIf="a.status !== 'CLOSED' && authService.hasPermission('ACCOUNT_CLOSE')"
                        class="btn btn-danger btn-sm"
                        (click)="closeAccount(a)"
                      >
                        Cerrar
                      </button>
                    </ng-container>
                  </div>
                </td>
              </tr>
              <tr *ngIf="accounts.length === 0">
                <td colspan="7" style="text-align: center; color: var(--gray-600); padding: 2rem;">
                  No se encontraron cuentas con los filtros seleccionados.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Modal Apertura de Cuenta -->
      <div class="modal-backdrop" *ngIf="showModal" (click)="closeModal()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <h2 style="margin-bottom: 1rem; color: var(--primary);">Apertura de Cuenta Bancaria</h2>

          <form (ngSubmit)="submitOpen()">
            <div class="form-group">
              <label class="form-label">Titular de la Cuenta (Cliente)</label>
              <select class="form-control" [(ngModel)]="newAccount.customerId" name="customerId" required>
                <option value="" disabled>Seleccione un cliente</option>
                <option *ngFor="let c of activeCustomers" [value]="c.id">
                  {{ c.fullName }} - {{ c.documentType }} {{ c.documentNumber }}
                </option>
              </select>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Tipo de Cuenta</label>
                <select class="form-control" [(ngModel)]="newAccount.accountType" name="accType" required>
                  <option value="SAVINGS">Ahorros (SAVINGS)</option>
                  <option value="CHECKING">Corriente (CHECKING)</option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Moneda</label>
                <select class="form-control" [(ngModel)]="newAccount.currency" name="currency" required>
                  <option value="COP">Pesos Colombianos (COP)</option>
                  <option value="USD">Dólares Americanos (USD)</option>
                </select>
              </div>
            </div>

            <p style="font-size: 0.8rem; color: var(--gray-600); margin-top: 0.5rem;">
              ℹ️ Toda cuenta nueva se apertura con saldo 0.00 y estado ACTIVE. El número de cuenta de 10 dígitos es generado automáticamente.
            </p>

            <div style="display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem;">
              <button type="button" class="btn btn-outline" (click)="closeModal()">Cancelar</button>
              <button type="submit" class="btn btn-primary" [disabled]="saving">
                {{ saving ? 'Aperturando...' : 'Abrir Cuenta' }}
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Modal de Extracto / Movimientos del Ledger -->
      <div class="modal-backdrop" *ngIf="showStatementModal" (click)="closeStatementModal()">
        <div class="modal-content" style="max-width: 750px;" (click)="$event.stopPropagation()">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
            <div>
              <h2 style="color: var(--primary);">Extracto de Cuenta</h2>
              <p style="font-size: 0.85rem; color: var(--gray-600);">
                Cuenta Nº <strong>{{ selectedAccount?.accountNumber }}</strong> ({{ selectedAccount?.type }})
              </p>
            </div>
            <div style="text-align: right;">
              <span style="font-size: 0.8rem; color: var(--gray-600);">Saldo Actual:</span>
              <div style="font-size: 1.25rem; font-weight: 700; color: var(--primary);">
                {{ selectedAccount?.balance | currency: selectedAccount?.currency : 'symbol' : '1.2-2' }}
              </div>
            </div>
          </div>

          <div *ngIf="movementsLoading" style="text-align: center; padding: 2rem;">
            Cargando movimientos del ledger...
          </div>

          <div *ngIf="!movementsLoading" class="table-container">
            <table>
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Tipo</th>
                  <th>Monto</th>
                  <th>Saldo Posterior</th>
                  <th>ID Asiento</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let m of movements">
                  <td>{{ m.createdAt | date:'short' }}</td>
                  <td>
                    <span
                      class="badge"
                      [ngClass]="(m.movementType || m.type) === 'CREDIT' ? 'badge-credit' : 'badge-debit'"
                    >
                      {{ (m.movementType || m.type) === 'CREDIT' ? 'CRÉDITO' : 'DÉBITO' }}
                    </span>
                  </td>
                  <td [style.color]="(m.movementType || m.type) === 'CREDIT' ? 'var(--success)' : 'var(--danger)'" style="font-weight: 600;">
                    {{ (m.movementType || m.type) === 'CREDIT' ? '+' : '-' }} {{ m.amount | currency: m.currency : 'symbol' : '1.2-2' }}
                  </td>
                  <td style="font-weight: 600;">
                    {{ m.balanceAfter | currency: m.currency : 'symbol' : '1.2-2' }}
                  </td>
                  <td style="font-size: 0.75rem; font-family: monospace; color: var(--gray-600);">
                    {{ m.id.substring(0, 8) }}...
                  </td>
                </tr>
                <tr *ngIf="movements.length === 0">
                  <td colspan="5" style="text-align: center; color: var(--gray-600); padding: 1.5rem;">
                    Esta cuenta aún no tiene movimientos contables registrados.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div style="display: flex; justify-content: flex-end; margin-top: 1.5rem;">
            <button class="btn btn-outline" (click)="closeStatementModal()">Cerrar</button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class AccountListComponent implements OnInit {
  accounts: Account[] = [];
  customers: Customer[] = [];
  activeCustomers: Customer[] = [];
  selectedCustomerId = '';
  selectedStatus = '';

  message = '';
  errorMessage = '';

  // Modal Apertura
  showModal = false;
  modalError = '';
  saving = false;
  newAccount: { customerId: string; accountType: AccountType; currency: CurrencyCode } = {
    customerId: '',
    accountType: 'SAVINGS',
    currency: 'COP'
  };

  // Modal Extracto
  showStatementModal = false;
  selectedAccount: Account | null = null;
  movements: Movement[] = [];
  movementsLoading = false;

  constructor(
    private accountService: AccountService,
    private customerService: CustomerService,
    private toastService: ToastService,
    public authService: AuthService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    if (this.authService.isCustomer()) {
      this.selectedCustomerId = this.authService.customerId() || '';
      this.loadAccounts();
    } else {
      this.route.queryParams.subscribe((params) => {
        if (params['customerId']) {
          this.selectedCustomerId = params['customerId'];
        }
        if (this.authService.hasPermission('CUSTOMER_READ')) {
          this.loadCustomers();
        }
        this.loadAccounts();
      });
    }
  }

  loadCustomers(): void {
    this.customerService.getAll(0, 100).subscribe({
      next: (res) => {
        this.customers = res.content;
        this.activeCustomers = res.content.filter((c) => c.status === 'ACTIVE');
      }
    });
  }

  loadAccounts(): void {
    this.accountService.getAll(
      this.selectedCustomerId || undefined,
      this.selectedStatus || undefined,
      0,
      100
    ).subscribe({
      next: (res) => (this.accounts = res.content),
      error: (err) => this.toastService.error('Error al cargar cuentas: ' + (err.error?.detail || err.message))
    });
  }

  openCreateModal(): void {
    this.showModal = true;
    this.modalError = '';
    this.newAccount = {
      customerId: this.selectedCustomerId || (this.activeCustomers[0]?.id ?? ''),
      accountType: 'SAVINGS',
      currency: 'COP'
    };
  }

  closeModal(): void {
    this.showModal = false;
  }

  submitOpen(): void {
    if (!this.newAccount.customerId) {
      this.modalError = 'Debes seleccionar un cliente titular.';
      return;
    }

    this.saving = true;
    this.modalError = '';

    this.accountService.open(this.newAccount).subscribe({
      next: (acc) => {
        this.saving = false;
        this.showModal = false;
        this.toastService.success(`Cuenta Nº ${acc.accountNumber} aperturada con éxito.`, 'Apertura Exitosa');
        this.loadAccounts();
      },
      error: (err) => {
        this.saving = false;
        const msg = err.error?.detail || 'Error al aperturar la cuenta.';
        this.modalError = msg;
        this.toastService.error(msg, 'Error en Apertura');
      }
    });
  }

  toggleStatus(account: Account, newStatus: 'ACTIVE' | 'BLOCKED'): void {
    const action = newStatus === 'BLOCKED' ? 'bloquear' : 'activar';
    const isBlocking = newStatus === 'BLOCKED';

    this.toastService.confirm(
      `¿Deseas ${action} la cuenta Nº ${account.accountNumber}? ${isBlocking ? 'No permitirá realizar débitos ni retiros.' : 'Se restablecerá la operación habitual.'}`,
      () => {
        this.accountService.updateStatus(account.id, newStatus).subscribe({
          next: () => {
            this.toastService.success(`Cuenta Nº ${account.accountNumber} ahora está ${newStatus}.`, 'Estado Actualizado');
            this.loadAccounts();
          },
          error: (err) => {
            const msg = err.error?.detail || 'No se pudo actualizar el estado de la cuenta.';
            this.toastService.error(msg, 'Error al Cambiar Estado');
          }
        });
      },
      {
        title: `Confirmar ${isBlocking ? 'Bloqueo' : 'Activación'}`,
        confirmText: isBlocking ? 'Bloquear' : 'Activar',
        cancelText: 'Cancelar',
        danger: isBlocking
      }
    );
  }

  closeAccount(account: Account): void {
    this.toastService.confirm(
      `¿Confirmas el cierre definitivo de la cuenta Nº ${account.accountNumber}? Debe tener saldo 0.00. Esta acción es irreversible.`,
      () => {
        this.accountService.close(account.id).subscribe({
          next: () => {
            this.toastService.success(`Cuenta Nº ${account.accountNumber} cerrada exitosamente.`, 'Cuenta Cerrada');
            this.loadAccounts();
          },
          error: (err) => {
            const msg = err.error?.detail || 'Error al cerrar cuenta (verifique que el saldo sea 0.00).';
            this.toastService.error(msg, 'Error al Cerrar Cuenta');
          }
        });
      },
      {
        title: 'Confirmar Cierre Definitivo',
        confirmText: 'Cerrar Cuenta',
        cancelText: 'Cancelar',
        danger: true
      }
    );
  }

  viewStatement(account: Account): void {
    this.selectedAccount = account;
    this.showStatementModal = true;
    this.movementsLoading = true;
    this.movements = [];

    this.accountService.getMovements(account.id, 0, 50).subscribe({
      next: (res) => {
        this.movements = res.content;
        this.movementsLoading = false;
      },
      error: () => {
        this.movementsLoading = false;
      }
    });
  }

  closeStatementModal(): void {
    this.showStatementModal = false;
    this.selectedAccount = null;
  }
}
