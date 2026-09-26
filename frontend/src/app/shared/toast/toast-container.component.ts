import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, Toast } from '../../core/services/toast.service';

@Component({
  selector: 'app-toast-container',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="toast-wrapper">
      <div
        *ngFor="let toast of toastService.toasts()"
        class="toast-card toast-{{ toast.type }}"
        role="alert"
      >
        <div class="toast-icon">
          <span *ngIf="toast.type === 'success'">✓</span>
          <span *ngIf="toast.type === 'error'">✕</span>
          <span *ngIf="toast.type === 'warning'">⚠</span>
          <span *ngIf="toast.type === 'info'">ℹ</span>
          <span *ngIf="toast.type === 'confirm'">❓</span>
        </div>

        <div class="toast-body">
          <strong *ngIf="toast.title" class="toast-title">{{ toast.title }}</strong>
          <p class="toast-message">{{ toast.message }}</p>

          <div *ngIf="toast.actions && toast.actions.length > 0" class="toast-actions">
            <button
              *ngFor="let act of toast.actions"
              type="button"
              class="toast-btn"
              [class.btn-danger]="act.danger"
              [class.btn-primary]="act.primary"
              [class.btn-secondary]="!act.primary && !act.danger"
              (click)="act.action()"
            >
              {{ act.label }}
            </button>
          </div>
        </div>

        <button class="toast-close" (click)="toastService.remove(toast.id)" aria-label="Cerrar">
          &times;
        </button>
      </div>
    </div>
  `,
  styles: [`
    .toast-wrapper {
      position: fixed;
      top: 1.25rem;
      right: 1.25rem;
      z-index: 99999;
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      pointer-events: none;
    }
    .toast-card {
      pointer-events: auto;
      display: flex;
      align-items: flex-start;
      gap: 0.75rem;
      width: 380px;
      max-width: 90vw;
      padding: 0.85rem 1rem;
      background: white;
      border-radius: var(--radius);
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
      border-left: 5px solid;
      animation: slideIn 0.25s cubic-bezier(0.16, 1, 0.3, 1);
      transition: all 0.2s ease-in-out;
    }
    @keyframes slideIn {
      from {
        transform: translateX(110%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }
    .toast-success {
      border-left-color: #16a34a;
    }
    .toast-success .toast-icon {
      background: #dcfce7;
      color: #16a34a;
    }
    .toast-error {
      border-left-color: #dc2626;
    }
    .toast-error .toast-icon {
      background: #fee2e2;
      color: #dc2626;
    }
    .toast-warning {
      border-left-color: #d97706;
    }
    .toast-warning .toast-icon {
      background: #fef3c7;
      color: #d97706;
    }
    .toast-info {
      border-left-color: #2563eb;
    }
    .toast-info .toast-icon {
      background: #dbeafe;
      color: #2563eb;
    }
    .toast-confirm {
      border-left-color: #d97706;
      background: #fffdfa;
      box-shadow: 0 12px 28px -4px rgba(217, 119, 6, 0.2), 0 8px 10px -6px rgba(0, 0, 0, 0.1);
    }
    .toast-confirm .toast-icon {
      background: #fef3c7;
      color: #b45309;
    }
    .toast-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 26px;
      height: 26px;
      border-radius: 50%;
      font-weight: 700;
      font-size: 0.85rem;
      flex-shrink: 0;
      margin-top: 2px;
    }
    .toast-body {
      flex: 1;
      font-size: 0.875rem;
      line-height: 1.35;
      color: var(--gray-700);
    }
    .toast-title {
      display: block;
      color: var(--gray-900);
      font-weight: 600;
      margin-bottom: 0.2rem;
    }
    .toast-message {
      margin: 0;
      word-break: break-word;
    }
    .toast-actions {
      display: flex;
      gap: 0.5rem;
      margin-top: 0.75rem;
      justify-content: flex-end;
    }
    .toast-btn {
      padding: 0.35rem 0.75rem;
      font-size: 0.8rem;
      font-weight: 600;
      border-radius: 4px;
      cursor: pointer;
      border: 1px solid transparent;
      transition: all 0.15s ease-in-out;
    }
    .toast-btn.btn-secondary {
      background: var(--gray-100);
      color: var(--gray-700);
      border-color: var(--gray-300);
    }
    .toast-btn.btn-secondary:hover {
      background: var(--gray-200);
      color: var(--gray-900);
    }
    .toast-btn.btn-primary {
      background: var(--primary);
      color: white;
    }
    .toast-btn.btn-primary:hover {
      background: var(--primary-hover);
    }
    .toast-btn.btn-danger {
      background: var(--danger);
      color: white;
    }
    .toast-btn.btn-danger:hover {
      background: #991b1b;
    }
    .toast-close {
      background: transparent;
      border: none;
      font-size: 1.25rem;
      line-height: 1;
      color: var(--gray-600);
      cursor: pointer;
      padding: 0 0.25rem;
      border-radius: 4px;
      transition: color 0.15s;
      flex-shrink: 0;
    }
    .toast-close:hover {
      color: var(--gray-900);
      background: var(--gray-100);
    }
  `]
})
export class ToastContainerComponent {
  constructor(public toastService: ToastService) {}
}
