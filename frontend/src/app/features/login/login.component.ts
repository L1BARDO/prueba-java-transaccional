import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

import { ToastService } from '../../core/services/toast.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-wrapper">
      <div class="login-card">
        <div class="login-header">
          <div class="logo">⚡</div>
          <h2>Switch Transaccional</h2>
          <p>Plataforma Bancaria de Alta Disponibilidad</p>
        </div>

        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label class="form-label" for="username">Usuario</label>
            <input
              id="username"
              type="text"
              class="form-control"
              [(ngModel)]="username"
              name="username"
              placeholder="Ingresa tu usuario"
              required
            />
          </div>

          <div class="form-group">
            <label class="form-label" for="password">Contraseña</label>
            <input
              id="password"
              type="password"
              class="form-control"
              [(ngModel)]="password"
              name="password"
              placeholder="••••••••"
              required
            />
          </div>

          <button type="submit" class="btn btn-primary btn-block" [disabled]="loading">
            {{ loading ? 'Autenticando...' : 'Iniciar Sesión' }}
          </button>
        </form>

        <div class="demo-section">
          <span class="demo-title">Acceso rápido con usuarios de prueba:</span>
          <div class="demo-buttons">
            <button type="button" class="btn btn-outline btn-sm" (click)="setCredentials('admin', 'Admin123*')">
              👑 Admin
            </button>
            <button type="button" class="btn btn-outline btn-sm" (click)="setCredentials('operador', 'Operador123*')">
              💼 Operador
            </button>
            <button type="button" class="btn btn-outline btn-sm" (click)="setCredentials('auditor', 'Auditor123*')">
              🔍 Auditor
            </button>
            <button type="button" class="btn btn-outline btn-sm" (click)="setCredentials('ana.perez', 'Cliente123*')">
              👤 Ana Pérez (Cliente)
            </button>
            <button type="button" class="btn btn-outline btn-sm" (click)="setCredentials('bloqueado', 'Bloqueado123*')">
              🔒 Bloqueado
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-wrapper {
      min-height: 80vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem 1rem;
    }
    .login-card {
      width: 100%;
      max-width: 440px;
      background: white;
      border: 1px solid var(--gray-200);
      border-radius: var(--radius);
      box-shadow: 0 10px 15px -3px rgb(0 0 0 / 0.1);
      padding: 2rem;
    }
    .login-header {
      text-align: center;
      margin-bottom: 1.5rem;
    }
    .logo {
      font-size: 2.5rem;
      margin-bottom: 0.5rem;
    }
    .login-header h2 {
      font-size: 1.5rem;
      color: var(--primary);
    }
    .login-header p {
      font-size: 0.85rem;
      color: var(--gray-600);
    }
    .btn-block {
      width: 100%;
      justify-content: center;
      padding: 0.65rem;
      margin-top: 0.5rem;
    }
    .demo-section {
      margin-top: 1.75rem;
      padding-top: 1.25rem;
      border-top: 1px solid var(--gray-200);
    }
    .demo-title {
      display: block;
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--gray-600);
      text-transform: uppercase;
      margin-bottom: 0.5rem;
    }
    .demo-buttons {
      display: flex;
      flex-wrap: wrap;
      gap: 0.4rem;
    }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  loading = false;

  constructor(
    private authService: AuthService,
    private toastService: ToastService,
    private router: Router
  ) {
    if (this.authService.isAuthenticated()) {
      this.router.navigate(['/dashboard']);
    }
  }

  setCredentials(user: string, pass: string): void {
    this.username = user;
    this.password = pass;
    this.toastService.info(`Credenciales cargadas para ${user}`);
  }

  onSubmit(): void {
    if (!this.username || !this.password) {
      this.toastService.warning('Por favor ingresa usuario y contraseña.');
      return;
    }

    this.loading = true;

    this.authService.login({ username: this.username, password: this.password }).subscribe({
      next: (res) => {
        this.loading = false;
        this.toastService.success(`Bienvenido, ${res.username}`, 'Inicio de Sesión Exitoso');
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.loading = false;
        const msg = err.error?.detail || (err.status === 401
          ? 'Credenciales inválidas. Verifica usuario y contraseña.'
          : err.status === 403
          ? 'Acceso denegado: el usuario se encuentra bloqueado.'
          : 'Error de conexión con el servidor.');
        this.toastService.error(msg, 'Error de Autenticación');
      }
    });
  }
}
