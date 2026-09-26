import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CustomerService } from '../../core/services/customer.service';
import { ToastService } from '../../core/services/toast.service';
import { Customer, DocumentType, RegisterCustomerRequest } from '../../core/models/models';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-customer-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container">
      <div class="card">
        <div class="card-header">
          <div>
            <h1 class="card-title">Gestión de Clientes</h1>
            <p style="font-size: 0.85rem; color: var(--gray-600);">Listado de clientes registrados en el sistema financiero.</p>
          </div>
          <button (click)="openCreateModal()" class="btn btn-primary">
            <span>+</span> Nuevo Cliente
          </button>
        </div>

        <div class="table-container">
          <table>
            <thead>
              <tr>
                <th>Documento</th>
                <th>Nombre Completo</th>
                <th>Correo Electrónico</th>
                <th>Teléfono</th>
                <th>Estado</th>
                <th>Fecha Registro</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let c of customers">
                <td><strong>{{ c.documentType }}</strong> {{ c.documentNumber }}</td>
                <td>{{ c.fullName }}</td>
                <td>{{ c.email }}</td>
                <td>{{ c.phone || 'N/A' }}</td>
                <td>
                  <span class="badge" [ngClass]="c.status === 'ACTIVE' ? 'badge-active' : 'badge-inactive'">
                    {{ c.status }}
                  </span>
                </td>
                <td>{{ c.createdAt | date:'short' }}</td>
                <td>
                  <div style="display: flex; gap: 0.5rem;">
                    <button class="btn btn-outline btn-sm" (click)="viewAccounts(c.id)">
                      Cuentas
                    </button>
                    <button
                      *ngIf="c.status === 'ACTIVE'"
                      class="btn btn-danger btn-sm"
                      (click)="deactivate(c)"
                    >
                      Baja
                    </button>
                  </div>
                </td>
              </tr>
              <tr *ngIf="customers.length === 0">
                <td colspan="7" style="text-align: center; color: var(--gray-600); padding: 2rem;">
                  No hay clientes registrados en este momento.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Modal para Registrar Cliente -->
      <div class="modal-backdrop" *ngIf="showModal" (click)="closeModal()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <h2 style="margin-bottom: 1rem; color: var(--primary);">Registrar Nuevo Cliente</h2>

          <form (ngSubmit)="submitCreate()">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Tipo de Documento</label>
                <select class="form-control" [(ngModel)]="newCustomer.documentType" name="docType" required>
                  <option value="CC">Cédula de Ciudadanía (CC)</option>
                  <option value="CE">Cédula de Extranjería (CE)</option>
                  <option value="NIT">NIT</option>
                  <option value="PASSPORT">Pasaporte</option>
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Número de Documento</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="newCustomer.documentNumber"
                  name="docNum"
                  placeholder="Ej: 1012345678"
                  required
                />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">Nombre Completo</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="newCustomer.fullName"
                name="fullName"
                placeholder="Ej: Laura Gómez"
                required
              />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Correo Electrónico</label>
                <input
                  type="email"
                  class="form-control"
                  [(ngModel)]="newCustomer.email"
                  name="email"
                  placeholder="laura@banco.com"
                  required
                />
              </div>

              <div class="form-group">
                <label class="form-label">Teléfono</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="newCustomer.phone"
                  name="phone"
                  placeholder="+57 300 1234567"
                />
              </div>
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem;">
              <button type="button" class="btn btn-outline" (click)="closeModal()">Cancelar</button>
              <button type="submit" class="btn btn-primary" [disabled]="saving">
                {{ saving ? 'Guardando...' : 'Crear Cliente' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `
})
export class CustomerListComponent implements OnInit {
  customers: Customer[] = [];
  message = '';
  errorMessage = '';
  showModal = false;
  modalError = '';
  saving = false;

  newCustomer: RegisterCustomerRequest = {
    documentType: 'CC',
    documentNumber: '',
    fullName: '',
    email: '',
    phone: ''
  };

  constructor(
    private customerService: CustomerService,
    private toastService: ToastService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (this.authService.isCustomer() || !this.authService.hasPermission('CUSTOMER_READ')) {
      this.toastService.warning('Módulo reservado para administración.', 'Acceso Restringido');
      this.router.navigate(['/dashboard']);
      return;
    }
    this.loadCustomers();
  }

  loadCustomers(): void {
    this.customerService.getAll(0, 50).subscribe({
      next: (res) => (this.customers = res.content),
      error: (err) => this.toastService.error('Error al cargar clientes: ' + (err.error?.detail || err.message))
    });
  }

  openCreateModal(): void {
    this.showModal = true;
    this.modalError = '';
    this.newCustomer = {
      documentType: 'CC',
      documentNumber: '',
      fullName: '',
      email: '',
      phone: ''
    };
  }

  closeModal(): void {
    this.showModal = false;
  }

  submitCreate(): void {
    this.saving = true;
    this.modalError = '';

    this.customerService.create(this.newCustomer).subscribe({
      next: (created) => {
        this.saving = false;
        this.showModal = false;
        this.toastService.success(`Cliente ${created.fullName} registrado con éxito.`, 'Cliente Creado');
        this.loadCustomers();
      },
      error: (err) => {
        this.saving = false;
        const msg = err.error?.detail || (err.error?.errors ? Object.values(err.error.errors).join(', ') : (err.error?.title || err.message));
        this.modalError = msg;
        this.toastService.error(msg, 'Error al Crear Cliente');
      }
    });
  }

  viewAccounts(customerId: string): void {
    this.router.navigate(['/accounts'], { queryParams: { customerId } });
  }

  deactivate(customer: Customer): void {
    this.toastService.confirm(
      `¿Confirmas la baja lógica del cliente ${customer.fullName}? Esta acción no se puede deshacer si tiene cuentas activas.`,
      () => {
        this.customerService.deactivate(customer.id).subscribe({
          next: () => {
            this.toastService.success(`Cliente ${customer.fullName} desactivado correctamente.`, 'Baja Procesada');
            this.loadCustomers();
          },
          error: (err) => {
            const msg = err.error?.detail || 'No se pudo desactivar el cliente (verifique que no tenga cuentas abiertas).';
            this.toastService.error(msg, 'Error al Desactivar');
          }
        });
      },
      {
        title: 'Confirmar Baja de Cliente',
        confirmText: 'Desactivar',
        cancelText: 'Cancelar',
        danger: true
      }
    );
  }
}
