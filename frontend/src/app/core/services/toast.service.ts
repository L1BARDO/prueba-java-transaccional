import { Injectable, signal } from '@angular/core';

export interface ToastAction {
  label: string;
  primary?: boolean;
  danger?: boolean;
  action: () => void;
}

export interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info' | 'confirm';
  title?: string;
  message: string;
  duration?: number;
  actions?: ToastAction[];
}

export interface ConfirmOptions {
  title?: string;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
  onCancel?: () => void;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private readonly toastsSignal = signal<Toast[]>([]);
  readonly toasts = this.toastsSignal.asReadonly();

  show(toast: Omit<Toast, 'id'>): string {
    const id = crypto.randomUUID();
    const newToast: Toast = {
      ...toast,
      id,
      duration: toast.duration ?? (toast.type === 'confirm' ? 0 : 4500)
    };

    this.toastsSignal.update((current) => [...current, newToast]);

    if (newToast.duration && newToast.duration > 0) {
      setTimeout(() => {
        this.remove(id);
      }, newToast.duration);
    }

    return id;
  }

  success(message: string, title = 'Operación Exitosa'): void {
    this.show({ type: 'success', title, message });
  }

  error(message: string, title = 'Error en Operación'): void {
    this.show({ type: 'error', title, message, duration: 6000 });
  }

  warning(message: string, title = 'Advertencia'): void {
    this.show({ type: 'warning', title, message });
  }

  info(message: string, title = 'Información'): void {
    this.show({ type: 'info', title, message });
  }

  /**
   * Muestra un toast alert interactivo de confirmación con botones de acción.
   */
  confirm(
    message: string,
    onConfirm: () => void,
    options?: ConfirmOptions
  ): void {
    const confirmText = options?.confirmText ?? 'Confirmar';
    const cancelText = options?.cancelText ?? 'Cancelar';
    const isDanger = options?.danger ?? false;

    let toastId = '';

    const actions: ToastAction[] = [
      {
        label: cancelText,
        primary: false,
        danger: false,
        action: () => {
          this.remove(toastId);
          options?.onCancel?.();
        }
      },
      {
        label: confirmText,
        primary: !isDanger,
        danger: isDanger,
        action: () => {
          this.remove(toastId);
          onConfirm();
        }
      }
    ];

    toastId = this.show({
      type: 'confirm',
      title: options?.title ?? 'Confirmar Acción',
      message,
      duration: 0,
      actions
    });
  }

  remove(id: string): void {
    this.toastsSignal.update((current) => current.filter((t) => t.id !== id));
  }
}
