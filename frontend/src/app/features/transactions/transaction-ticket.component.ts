import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TransactionService } from '../../core/services/transaction.service';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { Account, Transaction } from '../../core/models/models';

@Component({
  selector: 'app-transaction-ticket',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="ticket-page-wrapper">
      <!-- Barra de navegación y acciones (oculta al imprimir) -->
      <div class="ticket-actions-bar no-print">
        <button type="button" class="btn btn-back" (click)="goBack()">
          ← Volver a Transacciones
        </button>

        <div class="actions-group">
          <button type="button" class="btn btn-copy" (click)="copyReference()">
            {{ copied ? '✓ Referencia Copiada' : '📋 Copiar Referencia' }}
          </button>
          <button type="button" class="btn btn-print" (click)="printTicket()">
            🖨️ Imprimir Comprobante
          </button>
        </div>
      </div>

      <!-- Estado de carga -->
      <div *ngIf="loading" class="ticket-feedback no-print">
        <div class="spinner-dot"></div>
        <p>Generando comprobante de transacción...</p>
      </div>

      <!-- Estado de error -->
      <div *ngIf="!loading && error" class="ticket-error-box no-print">
        <div class="error-icon">✕</div>
        <h3>No se pudo cargar el comprobante</h3>
        <p>{{ error }}</p>
        <button type="button" class="btn btn-back" style="margin-top: 1rem;" (click)="goBack()">
          Regresar a Transacciones
        </button>
      </div>

      <!-- RECIBO / TICKET BANCARIO IMPRIMIBLE -->
      <div *ngIf="!loading && transaction" class="ticket-sheet" id="printable-ticket">
        <!-- Encabezado Institucional Bancario -->
        <header class="bank-header">
          <div class="bank-brand-row">
            <div class="bank-emblem">
              <span class="emblem-symbol">🏛️</span>
            </div>
            <div class="bank-info">
              <h1 class="bank-name">SWITCH TRANSACCIONAL S.A.</h1>
              <p class="bank-tagline">Sistema Central de Pagos y Liquidación Interbancaria</p>
              <p class="bank-legal">NIT: 901.845.239-1 · Vigilado Superintendencia Financiera de Colombia</p>
            </div>
          </div>
          <div class="doc-badge">
            <span class="doc-title">COMPROBANTE ELECTRÓNICO</span>
            <span class="doc-channel">Canal Digital / Switch Central</span>
          </div>
        </header>

        <div class="ticket-divider dashed"></div>

        <!-- Estado de la Operación -->
        <section class="status-section">
          <div
            class="status-banner"
            [ngClass]="transaction.status === 'COMPLETED' ? 'status-approved' : 'status-failed'"
          >
            <span class="status-icon">{{ transaction.status === 'COMPLETED' ? '✓' : '✕' }}</span>
            <div class="status-texts">
              <span class="status-heading">
                {{ transaction.status === 'COMPLETED' ? 'TRANSACCIÓN APROBADA' : 'TRANSACCIÓN NO EXITOSA' }}
              </span>
              <span class="status-sub">
                {{ transaction.status === 'COMPLETED' ? 'Operación procesada y registrada en el libro mayor' : 'La operación fue rechazada por el motor bancario' }}
              </span>
            </div>
          </div>
        </section>

        <!-- Monto Principal -->
        <section class="amount-hero">
          <span class="amount-caption">Monto Total de la Operación</span>
          <div class="amount-number">
            {{ transaction.amount | currency: transaction.currency : 'symbol' : '1.2-2' }}
            <span class="currency-tag">{{ transaction.currency }}</span>
          </div>
          <div class="amount-fee">
            <span>Costo de transacción: <strong>$0.00 COP</strong> (Tarifa cero)</span>
          </div>
        </section>

        <div class="ticket-divider solid"></div>

        <!-- Ficha de Datos Técnicos y Financieros -->
        <section class="details-section">
          <h2 class="section-title">Detalles de la Operación</h2>

          <div class="details-grid">
            <div class="detail-row">
              <span class="detail-lbl">Referencia Única:</span>
              <span class="detail-val mono-val"><strong>{{ transaction.reference }}</strong></span>
            </div>

            <div class="detail-row">
              <span class="detail-lbl">Fecha y Hora:</span>
              <span class="detail-val">{{ transaction.createdAt | date:'dd/MM/yyyy - HH:mm:ss' }}</span>
            </div>

            <div class="detail-row">
              <span class="detail-lbl">Tipo de Operación:</span>
              <span class="detail-val"><strong>{{ getOperationTypeName(transaction.type) }}</strong></span>
            </div>

            <div class="detail-row">
              <span class="detail-lbl">Concepto / Descripción:</span>
              <span class="detail-val">{{ transaction.description || 'Operación bancaria electrónica' }}</span>
            </div>

            <!-- CUENTA ORIGEN (Débito) -->
            <div class="account-card-box" *ngIf="transaction.sourceAccountId || sourceAccount">
              <div class="box-head">
                <span class="box-title">📤 Cuenta de Origen (Débito)</span>
              </div>
              <div class="box-content">
                <div class="card-line" *ngIf="sourceAccount?.customerName">
                  <span class="line-k">Titular:</span>
                  <span class="line-v font-bold">{{ sourceAccount?.customerName }}</span>
                </div>
                <div class="card-line" *ngIf="sourceAccount?.customerDocumentNumber">
                  <span class="line-k">Documento:</span>
                  <span class="line-v">CC {{ sourceAccount?.customerDocumentNumber }}</span>
                </div>
                <div class="card-line">
                  <span class="line-k">Nº Cuenta:</span>
                  <span class="line-v mono-val">
                    {{ sourceAccount?.accountNumber ? ('Cuenta ' + sourceAccount?.accountNumber) : transaction.sourceAccountId }}
                  </span>
                </div>
                <div class="card-line" *ngIf="sourceAccount">
                  <span class="line-k">Tipo / Divisa:</span>
                  <span class="line-v">
                    {{ sourceAccount.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }} ({{ sourceAccount.currency }})
                  </span>
                </div>
              </div>
            </div>

            <!-- CUENTA DESTINO (Crédito) -->
            <div class="account-card-box" *ngIf="transaction.destinationAccountId || destAccount">
              <div class="box-head">
                <span class="box-title">📥 Cuenta Destino (Crédito)</span>
              </div>
              <div class="box-content">
                <div class="card-line" *ngIf="destAccount?.customerName">
                  <span class="line-k">Beneficiario:</span>
                  <span class="line-v font-bold">{{ destAccount?.customerName }}</span>
                </div>
                <div class="card-line" *ngIf="destAccount?.customerDocumentNumber">
                  <span class="line-k">Documento:</span>
                  <span class="line-v">CC {{ destAccount?.customerDocumentNumber }}</span>
                </div>
                <div class="card-line">
                  <span class="line-k">Nº Cuenta:</span>
                  <span class="line-v mono-val">
                    {{ destAccount?.accountNumber ? ('Cuenta ' + destAccount?.accountNumber) : transaction.destinationAccountId }}
                  </span>
                </div>
                <div class="card-line" *ngIf="destAccount">
                  <span class="line-k">Tipo / Divisa:</span>
                  <span class="line-v">
                    {{ destAccount.type === 'SAVINGS' ? 'Ahorros' : 'Corriente' }} ({{ destAccount.currency }})
                  </span>
                </div>
              </div>
            </div>

            <!-- Motivo de Rechazo en caso de fallar -->
            <div class="rejection-box" *ngIf="transaction.status === 'REJECTED'">
              <span class="rej-k">Código de Error:</span>
              <span class="rej-v"><code>{{ transaction.failureCode }}</code></span>
              <span class="rej-k">Motivo:</span>
              <span class="rej-v">{{ transaction.failureReason }}</span>
            </div>
          </div>
        </section>

        <div class="ticket-divider dashed"></div>

        <!-- Sello de Seguridad y Código de Verificación -->
        <footer class="security-footer">
          <div class="security-meta">
            <div class="security-seal">
              <span class="seal-label">SELLO DIGITAL DE SEGURIDAD / ID</span>
              <code class="seal-hash">{{ transaction.id }}</code>
            </div>
            <p class="legal-notice">
              Este recibo electrónico certifica la ejecución irreversible de la transacción financiera según los protocolos de auditoría bancaria y el estándar ISO 20022. Para consultas o aclaraciones conserve el número de referencia.
            </p>
          </div>

          <!-- Código QR / Barras Simulado para Validación -->
          <div class="barcode-visual">
            <div class="barcode-lines">
              <span class="bar b-thick"></span><span class="bar b-thin"></span>
              <span class="bar b-med"></span><span class="bar b-thin"></span>
              <span class="bar b-thick"></span><span class="bar b-med"></span>
              <span class="bar b-thin"></span><span class="bar b-thick"></span>
              <span class="bar b-med"></span><span class="bar b-thin"></span>
              <span class="bar b-thick"></span><span class="bar b-thin"></span>
              <span class="bar b-med"></span><span class="bar b-thick"></span>
              <span class="bar b-thin"></span><span class="bar b-med"></span>
              <span class="bar b-thick"></span><span class="bar b-thin"></span>
              <span class="bar b-med"></span><span class="bar b-thick"></span>
            </div>
            <span class="barcode-sub">{{ transaction.reference }}</span>
          </div>
        </footer>
      </div>
    </div>
  `,
  styles: [`
    .ticket-page-wrapper {
      max-width: 760px;
      margin: 1.5rem auto 3rem;
      padding: 0 1rem;
    }

    /* Barra Superior de Acciones */
    .ticket-actions-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
      flex-wrap: wrap;
      gap: 0.75rem;
    }
    .actions-group {
      display: flex;
      gap: 0.75rem;
    }
    .btn {
      padding: 0.6rem 1.1rem;
      border-radius: var(--radius);
      font-weight: 600;
      font-size: 0.875rem;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 0.4rem;
      transition: all 0.2s;
    }
    .btn-back {
      background: white;
      border: 1px solid var(--gray-300);
      color: var(--gray-700);
    }
    .btn-back:hover {
      background: var(--gray-100);
      color: var(--gray-900);
    }
    .btn-copy {
      background: white;
      border: 1px solid var(--gray-300);
      color: var(--primary);
    }
    .btn-copy:hover {
      background: var(--primary-light);
    }
    .btn-print {
      background: var(--primary);
      border: 1px solid var(--primary);
      color: white;
      box-shadow: 0 2px 4px rgba(30, 58, 138, 0.2);
    }
    .btn-print:hover {
      background: var(--primary-hover);
    }

    /* Estado de Carga y Error */
    .ticket-feedback {
      background: white;
      border-radius: var(--radius);
      padding: 3rem;
      text-align: center;
      color: var(--gray-600);
      box-shadow: var(--shadow);
    }
    .spinner-dot {
      width: 32px;
      height: 32px;
      border: 3px solid var(--gray-200);
      border-top-color: var(--primary);
      border-radius: 50%;
      margin: 0 auto 1rem;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    .ticket-error-box {
      background: white;
      border-radius: var(--radius);
      padding: 2.5rem;
      text-align: center;
      border: 1px solid var(--danger);
      box-shadow: var(--shadow);
    }
    .error-icon {
      font-size: 2.5rem;
      color: var(--danger);
      margin-bottom: 0.5rem;
    }

    /* LA HOJA / TICKET IMPRIMIBLE */
    .ticket-sheet {
      background: white;
      border: 1px solid var(--gray-300);
      border-radius: var(--radius);
      padding: 2.25rem 2.5rem;
      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.08), 0 8px 10px -6px rgba(0, 0, 0, 0.04);
      position: relative;
    }

    /* Encabezado Institucional */
    .bank-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 1.5rem;
      flex-wrap: wrap;
    }
    .bank-brand-row {
      display: flex;
      gap: 1rem;
      align-items: center;
    }
    .bank-emblem {
      background: #eff6ff;
      border: 1px solid #bfdbfe;
      width: 48px;
      height: 48px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.6rem;
    }
    .bank-name {
      font-size: 1.25rem;
      font-weight: 800;
      color: var(--primary);
      letter-spacing: -0.3px;
      margin: 0;
      line-height: 1.2;
    }
    .bank-tagline {
      font-size: 0.8rem;
      color: var(--gray-700);
      font-weight: 600;
      margin-top: 0.15rem;
    }
    .bank-legal {
      font-size: 0.7rem;
      color: var(--gray-600);
      margin-top: 0.1rem;
    }
    .doc-badge {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      text-align: right;
    }
    .doc-title {
      font-size: 0.85rem;
      font-weight: 700;
      background: var(--gray-100);
      color: var(--gray-800);
      padding: 0.25rem 0.6rem;
      border-radius: 4px;
      letter-spacing: 0.5px;
    }
    .doc-channel {
      font-size: 0.725rem;
      color: var(--gray-600);
      margin-top: 0.25rem;
    }

    /* Divisores */
    .ticket-divider {
      margin: 1.5rem 0;
      width: 100%;
    }
    .ticket-divider.dashed {
      border-top: 1px dashed var(--gray-300);
    }
    .ticket-divider.solid {
      border-top: 1px solid var(--gray-200);
    }

    /* Estado de la Transacción */
    .status-banner {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 0.85rem 1.25rem;
      border-radius: var(--radius);
    }
    .status-approved {
      background: #ecfdf5;
      border: 1px solid #a7f3d0;
      color: #065f46;
    }
    .status-failed {
      background: #fef2f2;
      border: 1px solid #fecaca;
      color: #991b1b;
    }
    .status-icon {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      font-size: 1.1rem;
      flex-shrink: 0;
    }
    .status-approved .status-icon {
      background: #10b981;
      color: white;
    }
    .status-failed .status-icon {
      background: #ef4444;
      color: white;
    }
    .status-texts {
      display: flex;
      flex-direction: column;
    }
    .status-heading {
      font-weight: 800;
      font-size: 0.95rem;
      letter-spacing: 0.5px;
    }
    .status-sub {
      font-size: 0.775rem;
      opacity: 0.9;
    }

    /* Monto Hero */
    .amount-hero {
      text-align: center;
      padding: 0.5rem 0;
    }
    .amount-caption {
      font-size: 0.8rem;
      text-transform: uppercase;
      letter-spacing: 0.8px;
      color: var(--gray-600);
      font-weight: 600;
    }
    .amount-number {
      font-size: 2.25rem;
      font-weight: 800;
      color: #0f172a;
      line-height: 1.2;
      margin: 0.25rem 0;
      display: flex;
      align-items: baseline;
      justify-content: center;
      gap: 0.5rem;
    }
    .currency-tag {
      font-size: 1.1rem;
      font-weight: 700;
      color: var(--primary);
    }
    .amount-fee {
      font-size: 0.8rem;
      color: var(--gray-600);
    }

    /* Detalles de la Operación */
    .section-title {
      font-size: 0.95rem;
      font-weight: 700;
      color: var(--gray-700);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      margin-bottom: 1rem;
    }
    .details-grid {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }
    .detail-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 0.875rem;
      padding-bottom: 0.4rem;
      border-bottom: 1px dotted var(--gray-200);
    }
    .detail-lbl {
      color: var(--gray-600);
      font-weight: 500;
    }
    .detail-val {
      color: var(--gray-900);
      text-align: right;
    }
    .mono-val {
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 0.9rem;
    }

    /* Tarjetas de Cuentas involucradas */
    .account-card-box {
      margin-top: 0.5rem;
      background: var(--gray-50);
      border: 1px solid var(--gray-200);
      border-radius: var(--radius);
      padding: 0.85rem 1rem;
    }
    .box-head {
      border-bottom: 1px solid var(--gray-200);
      padding-bottom: 0.35rem;
      margin-bottom: 0.5rem;
    }
    .box-title {
      font-weight: 700;
      font-size: 0.8rem;
      color: var(--primary);
      text-transform: uppercase;
      letter-spacing: 0.4px;
    }
    .box-content {
      display: flex;
      flex-direction: column;
      gap: 0.3rem;
    }
    .card-line {
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-size: 0.825rem;
    }
    .line-k {
      color: var(--gray-600);
    }
    .line-v {
      color: var(--gray-900);
    }
    .font-bold {
      font-weight: 700;
    }

    /* Caja de rechazo */
    .rejection-box {
      margin-top: 0.5rem;
      background: #fef2f2;
      border: 1px solid #fecaca;
      border-radius: var(--radius);
      padding: 0.75rem 1rem;
      font-size: 0.85rem;
      display: grid;
      grid-template-columns: auto 1fr;
      gap: 0.4rem 0.8rem;
    }
    .rej-k {
      font-weight: 700;
      color: #991b1b;
    }
    .rej-v {
      color: #7f1d1d;
    }

    /* Pie de Seguridad y Código de Barras */
    .security-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 1.5rem;
      flex-wrap: wrap;
    }
    .security-meta {
      flex: 1;
      min-width: 260px;
    }
    .security-seal {
      margin-bottom: 0.4rem;
    }
    .seal-label {
      display: block;
      font-size: 0.65rem;
      font-weight: 700;
      color: var(--gray-600);
      letter-spacing: 0.6px;
    }
    .seal-hash {
      font-size: 0.725rem;
      color: var(--gray-700);
      word-break: break-all;
    }
    .legal-notice {
      font-size: 0.675rem;
      color: var(--gray-600);
      line-height: 1.35;
      margin: 0;
    }

    /* Código de Barras Simulado */
    .barcode-visual {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.35rem;
      padding: 0.5rem 0.75rem;
      background: var(--gray-50);
      border: 1px solid var(--gray-200);
      border-radius: 4px;
    }
    .barcode-lines {
      display: flex;
      align-items: center;
      height: 38px;
      gap: 2px;
    }
    .bar {
      height: 100%;
      background: #0f172a;
      display: inline-block;
    }
    .b-thin { width: 1.5px; }
    .b-med { width: 3px; }
    .b-thick { width: 5px; }
    .barcode-sub {
      font-size: 0.65rem;
      font-family: ui-monospace, monospace;
      color: var(--gray-700);
    }

    /* ESTILOS PARA IMPRESIÓN (window.print()) */
    @media print {
      /* Ocultar elementos no imprimibles de toda la aplicación */
      .no-print,
      app-navbar,
      nav,
      .navbar,
      app-toast-container {
        display: none !important;
      }

      body, html {
        background: white !important;
        margin: 0 !important;
        padding: 0 !important;
      }

      .ticket-page-wrapper {
        max-width: 100% !important;
        margin: 0 !important;
        padding: 0 !important;
      }

      .ticket-sheet {
        box-shadow: none !important;
        border: 1px solid #000 !important;
        border-radius: 0 !important;
        padding: 1.5rem !important;
        width: 100% !important;
      }

      .status-banner {
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
      }
      .amount-hero {
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
      }
      .account-card-box {
        -webkit-print-color-adjust: exact;
        print-color-adjust: exact;
      }
    }
  `]
})
export class TransactionTicketComponent implements OnInit {
  transaction: Transaction | null = null;
  sourceAccount: Account | null = null;
  destAccount: Account | null = null;
  loading = true;
  error: string | null = null;
  copied = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private transactionService: TransactionService,
    private accountService: AccountService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    const txId = this.route.snapshot.paramMap.get('id');
    if (!txId) {
      this.error = 'No se especificó un identificador de transacción.';
      this.loading = false;
      return;
    }

    this.transactionService.getById(txId).subscribe({
      next: (tx) => {
        this.transaction = tx;
        this.loading = false;

        // Cargar información de cuenta origen
        if (tx.sourceAccountId) {
          this.accountService.getById(tx.sourceAccountId).subscribe({
            next: (acc) => (this.sourceAccount = acc),
            error: () => {}
          });
        }

        // Cargar información de cuenta destino
        if (tx.destinationAccountId) {
          this.accountService.getById(tx.destinationAccountId).subscribe({
            next: (acc) => (this.destAccount = acc),
            error: () => {}
          });
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = err.error?.detail || err.error?.title || 'No se encontró la transacción solicitada.';
      }
    });
  }

  getOperationTypeName(type?: string): string {
    switch (type) {
      case 'DEPOSIT':
        return 'Depósito en Efectivo / Ventanilla';
      case 'WITHDRAWAL':
        return 'Retiro de Fondos / Cajero';
      case 'TRANSFER':
        return 'Transferencia Atómica Entre Cuentas';
      default:
        return type || 'Transacción Bancaria';
    }
  }

  printTicket(): void {
    window.print();
  }

  goBack(): void {
    this.router.navigate(['/transactions'], { queryParams: { tab: 'HISTORY' } });
  }

  copyReference(): void {
    if (this.transaction?.reference) {
      navigator.clipboard.writeText(this.transaction.reference).then(() => {
        this.copied = true;
        setTimeout(() => (this.copied = false), 2500);
      });
    }
  }
}
