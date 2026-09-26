import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Router } from '@angular/router';
import { AuthResponse, LoginRequest, UserSession } from '../models/models';

const SESSION_KEY = 'switchtx_session';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly sessionSignal = signal<UserSession | null>(this.loadSession());

  readonly currentUser = this.sessionSignal.asReadonly();
  readonly isAuthenticated = computed(() => !!this.sessionSignal());
  readonly username = computed(() => this.sessionSignal()?.username ?? '');
  readonly fullName = computed(() => this.sessionSignal()?.fullName ?? this.username());
  readonly email = computed(() => this.sessionSignal()?.email ?? '');
  readonly customerId = computed(() => this.sessionSignal()?.customerId ?? null);
  readonly roles = computed(() => this.sessionSignal()?.roles ?? []);
  readonly permissions = computed(() => this.sessionSignal()?.permissions ?? []);
  readonly isCustomer = computed(() => this.roles().includes('CUSTOMER') || !!this.customerId());

  constructor(private http: HttpClient, private router: Router) {}

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/login', credentials).pipe(
      tap((res) => {
        const session: UserSession = {
          userId: res.userId,
          username: res.username,
          fullName: res.fullName,
          email: res.email,
          customerId: res.customerId ?? null,
          token: res.token,
          roles: res.roles,
          permissions: res.permissions
        };
        localStorage.setItem(SESSION_KEY, JSON.stringify(session));
        this.sessionSignal.set(session);
      })
    );
  }

  logout(): void {
    localStorage.removeItem(SESSION_KEY);
    this.sessionSignal.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return this.sessionSignal()?.token ?? null;
  }

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  hasPermission(permission: string): boolean {
    return this.permissions().includes(permission);
  }

  private loadSession(): UserSession | null {
    const raw = localStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as UserSession;
    } catch {
      localStorage.removeItem(SESSION_KEY);
      return null;
    }
  }
}
