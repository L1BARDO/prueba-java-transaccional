import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar" *ngIf="authService.isAuthenticated()">
      <div class="nav-container">
        <div class="nav-brand">
          <strong>Switch Transaccional</strong>
          <span *ngIf="authService.isCustomer()" class="portal-badge">Portal Cliente</span>
        </div>

        <div class="nav-links">
          <!-- Vista para Clientes -->
          <ng-container *ngIf="authService.isCustomer()">
            <a routerLink="/dashboard" routerLinkActive="active" class="nav-link">
              Mi Portal
            </a>
            <a routerLink="/accounts" routerLinkActive="active" class="nav-link">
              Mis Cuentas
            </a>
            <a routerLink="/transactions" routerLinkActive="active" class="nav-link">
              Transferencias
            </a>
          </ng-container>

          <!-- Vista para Administrativos (Admin, Operador, Auditor) -->
          <ng-container *ngIf="!authService.isCustomer()">
            <a routerLink="/dashboard" routerLinkActive="active" class="nav-link">
              Inicio
            </a>
            <a
              *ngIf="authService.hasPermission('CUSTOMER_READ')"
              routerLink="/customers"
              routerLinkActive="active"
              class="nav-link"
            >
              Clientes
            </a>
            <a routerLink="/accounts" routerLinkActive="active" class="nav-link">
              Cuentas
            </a>
            <a routerLink="/transactions" routerLinkActive="active" class="nav-link">
              Transacciones
            </a>
          </ng-container>
        </div>

        <div class="nav-user">
          <div class="user-info">
            <span class="user-fullname">{{ authService.fullName() }}</span>
            <div class="role-tags">
              <span class="user-role" *ngFor="let role of authService.roles()">{{ role }}</span>
            </div>
          </div>
          <button (click)="logout()" class="btn btn-outline btn-sm">Cerrar Sesión</button>
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      background: white;
      border-bottom: 1px solid var(--gray-200);
      position: sticky;
      top: 0;
      z-index: 100;
      box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.05);
    }
    .nav-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0.75rem 1.5rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      flex-wrap: wrap;
      gap: 1rem;
    }
    .nav-brand {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 1.1rem;
      color: var(--primary);
    }
    .brand-icon {
      font-size: 1.3rem;
    }
    .portal-badge {
      background: #eff6ff;
      color: #1d4ed8;
      border: 1px solid #bfdbfe;
      font-size: 0.75rem;
      padding: 0.15rem 0.5rem;
      border-radius: 9999px;
      font-weight: 600;
    }
    .nav-links {
      display: flex;
      gap: 1rem;
      align-items: center;
    }
    .nav-link {
      color: var(--gray-600);
      font-weight: 500;
      font-size: 0.9rem;
      padding: 0.4rem 0.75rem;
      border-radius: var(--radius);
      transition: all 0.2s;
      display: inline-flex;
      align-items: center;
      gap: 0.35rem;
    }
    .nav-link:hover, .nav-link.active {
      color: var(--primary);
      background-color: var(--primary-light);
      text-decoration: none;
      font-weight: 600;
    }
    .nav-user {
      display: flex;
      align-items: center;
      gap: 1rem;
    }
    .user-info {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
    }
    .user-fullname {
      font-weight: 600;
      font-size: 0.875rem;
      color: var(--gray-900);
    }
    .role-tags {
      display: flex;
      gap: 0.25rem;
    }
    .user-role {
      font-size: 0.675rem;
      color: var(--primary);
      background: var(--primary-light);
      padding: 0.1rem 0.4rem;
      border-radius: 4px;
      margin-top: 2px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
  `]
})
export class NavbarComponent {
  constructor(public authService: AuthService) {}

  logout(): void {
    this.authService.logout();
  }
}
