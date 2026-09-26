import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CustomerService } from '../../core/services/customer.service';
import { ToastService } from '../../core/services/toast.service';
import { Customer, DocumentType, RegisterCustomerRequest, UpdateCustomerRequest } from '../../core/models/models';
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
          <button
            *ngIf="authService.hasPermission('CUSTOMER_CREATE')"
            (click)="openCreateModal()"
            class="btn btn-primary"
          >
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
                  <div style="display: flex; gap: 0.4rem; align-items: center;">
                    <button class="btn btn-outline btn-sm" (click)="viewAccounts(c.id)" title="Ver cuentas">
                      Cuentas
                    </button>
                    <button
                      *ngIf="authService.hasPermission('CUSTOMER_UPDATE')"
                      class="btn btn-primary btn-sm"
                      (click)="openEditModal(c)"
                      title="Editar datos del cliente y contraseña"
                    >
                      Editar
                    </button>
                    <button
                      *ngIf="c.status === 'ACTIVE' && authService.hasPermission('CUSTOMER_DELETE')"
                      class="btn btn-danger btn-sm"
                      (click)="deactivate(c)"
                      title="Dar de baja lógica"
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
          <div class="modal-header">
            <h2 class="modal-title">Registrar Nuevo Cliente</h2>
            <button type="button" class="btn-close" (click)="closeModal()">✕</button>
          </div>

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
                  placeholder="1020304050"
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
                placeholder="Laura Gómez"
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

      <!-- Modal para Editar Cliente (incluye Contraseña) -->
      <div class="modal-backdrop" *ngIf="showEditModal" (click)="closeEditModal()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h2 class="modal-title">Editar Cliente</h2>
            <button type="button" class="btn-close" (click)="closeEditModal()" title="Cerrar modal">✕</button>
          </div>
          <p style="font-size: 0.85rem; color: var(--gray-600); margin-bottom: 1.25rem;">
            Actualiza los datos de contacto y/o la contraseña de acceso al portal bancario del cliente.
          </p>

          <form (ngSubmit)="submitEdit()">
            <!-- Documento (Informativo / Inmutable por regla de negocio) -->
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Tipo Documento</label>
                <input
                  type="text"
                  class="form-control"
                  [value]="selectedCustomer?.documentType"
                  disabled
                  style="background: var(--gray-100);"
                />
              </div>

              <div class="form-group">
                <label class="form-label">Número de Documento</label>
                <input
                  type="text"
                  class="form-control"
                  [value]="selectedCustomer?.documentNumber"
                  disabled
                  style="background: var(--gray-100);"
                />
              </div>
            </div>

            <div class="form-group">
              <label class="form-label">Nombre Completo *</label>
              <input
                type="text"
                class="form-control"
                [(ngModel)]="editForm.fullName"
                name="editFullName"
                placeholder="Nombre y Apellidos"
                required
              />
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">Correo Electrónico *</label>
                <input
                  type="email"
                  class="form-control"
                  [(ngModel)]="editForm.email"
                  name="editEmail"
                  placeholder="cliente@mail.com"
                  required
                />
              </div>

              <div class="form-group">
                <label class="form-label">Teléfono Móvil</label>
                <input
                  type="text"
                  class="form-control"
                  [(ngModel)]="editForm.phone"
                  name="editPhone"
                  placeholder="+573001234567"
                />
              </div>
            </div>

            <!-- Contraseña opcional -->
            <div class="password-box">
              <label class="form-label" style="display: flex; align-items: center; justify-content: space-between;">
                <span>🔑 Contraseña del Portal Bancario</span>
                <small style="color: var(--gray-600); font-weight: normal;">(Opcional, dejar vacío si no cambia)</small>
              </label>
              <input
                type="password"
                class="form-control"
                [(ngModel)]="editForm.password"
                name="editPassword"
                placeholder="Ingresar nueva contraseña para actualizar"
                minlength="6"
              />
              <small style="display: block; margin-top: 0.35rem; color: var(--gray-600); font-size: 0.775rem;">
                Mínimo 6 caracteres. Si se especifica, se actualizarán las credenciales de inicio de sesión del cliente.
              </small>
            </div>

            <div style="display: flex; justify-content: flex-end; gap: 0.75rem; margin-top: 1.5rem;">
              <button type="button" class="btn btn-outline" (click)="closeEditModal()">Cancelar</button>
              <button type="submit" class="btn btn-primary" [disabled]="saving">
                {{ saving ? 'Guardando...' : 'Guardar Cambios' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 0.5rem;
    }
    .modal-title {
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--gray-900);
      margin: 0;
    }
    .btn-close {
      background: transparent;
      border: none;
      font-size: 1.25rem;
      cursor: pointer;
      color: var(--gray-600);
      padding: 0.25rem;
      line-height: 1;
      border-radius: 4px;
    }
    .btn-close:hover {
      color: var(--gray-900);
      background: var(--gray-100);
    }
    .password-box {
      background: #f8fafc;
      border: 1px solid var(--gray-200);
      border-radius: var(--radius);
      padding: 0.85rem 1rem;
      margin-top: 0.75rem;
    }
  `]
})
export class CustomerListComponent implements OnInit {
  customers: Customer[] = [];
  saving = false;

  // Modal Crear
  showModal = false;
  newCustomer: RegisterCustomerRequest = {
    documentType: 'CC',
    documentNumber: '',
    fullName: '',
    email: '',
    phone: ''
  };

  // Modal Editar
  showEditModal = false;
  selectedCustomer: Customer | null = null;
  editForm: UpdateCustomerRequest = {
    fullName: '',
    email: '',
    phone: '',
    password: ''
  };

  constructor(
    private customerService: CustomerService,
    private toastService: ToastService,
    public authService: AuthService,
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
    if (!this.newCustomer.documentNumber || !this.newCustomer.fullName || !this.newCustomer.email) {
      this.toastService.warning('Por favor complete los campos obligatorios.', 'Validación');
      return;
    }

    this.saving = true;

    const payload: RegisterCustomerRequest = {
      documentType: this.newCustomer.documentType,
      documentNumber: this.newCustomer.documentNumber.trim(),
      fullName: this.newCustomer.fullName.trim(),
      email: this.newCustomer.email.trim(),
      phone: this.newCustomer.phone?.trim() ? this.newCustomer.phone.trim().replace(/[\s\-]/g, '') : undefined
    };

    this.customerService.create(payload).subscribe({
      next: (created) => {
        this.saving = false;
        this.showModal = false;
        this.toastService.success(`Cliente ${created.fullName} registrado con éxito.`, 'Cliente Creado');
        this.loadCustomers();
      },
      error: (err) => {
        this.saving = false;
        const msg = err.error?.detail || (err.error?.errors ? Object.values(err.error.errors).join(', ') : (err.error?.title || err.message));
        this.toastService.error(msg, 'Error al Crear Cliente');
      }
    });
  }

  openEditModal(customer: Customer): void {
    this.selectedCustomer = customer;
    this.editForm = {
      fullName: customer.fullName,
      email: customer.email,
      phone: customer.phone || '',
      password: ''
    };
    this.showEditModal = true;
  }

  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedCustomer = null;
  }

  submitEdit(): void {
    if (!this.selectedCustomer) return;

    if (!this.editForm.fullName || !this.editForm.email) {
      this.toastService.warning('El nombre completo y el correo electrónico son obligatorios.', 'Validación');
      return;
    }

    if (this.editForm.password && this.editForm.password.length < 6) {
      this.toastService.warning('La contraseña debe tener al menos 6 caracteres.', 'Validación');
      return;
    }

    this.saving = true;

    const payload: UpdateCustomerRequest = {
      fullName: this.editForm.fullName.trim(),
      email: this.editForm.email.trim(),
      phone: this.editForm.phone?.trim() ? this.editForm.phone.trim().replace(/[\s\-]/g, '') : undefined,
      password: this.editForm.password && this.editForm.password.trim().length > 0 ? this.editForm.password.trim() : undefined
    };

    this.customerService.update(this.selectedCustomer.id, payload).subscribe({
      next: (updated) => {
        this.saving = false;
        this.showEditModal = false;
        const pwdMsg = payload.password ? ' y contraseña actualizada' : '';
        this.toastService.success(
          `Cliente ${updated.fullName} actualizado exitosamente${pwdMsg}.`,
          'Cliente Actualizado'
        );
        this.loadCustomers();
      },
      error: (err) => {
        this.saving = false;
        const msg = err.error?.detail || err.error?.title || err.message || 'Error al actualizar cliente.';
        this.toastService.error(msg, 'Error al Actualizar');
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
