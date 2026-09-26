import { Routes } from '@angular/router';
import { LoginComponent } from './features/login/login.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { CustomerListComponent } from './features/customers/customer-list.component';
import { AccountListComponent } from './features/accounts/account-list.component';
import { TransactionOperationsComponent } from './features/transactions/transaction-operations.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'customers', component: CustomerListComponent, canActivate: [authGuard] },
  { path: 'accounts', component: AccountListComponent, canActivate: [authGuard] },
  { path: 'transactions', component: TransactionOperationsComponent, canActivate: [authGuard] },
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: '/dashboard' }
];
