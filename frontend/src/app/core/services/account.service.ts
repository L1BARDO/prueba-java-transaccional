import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Account, AccountStatus, Movement, OpenAccountRequest, PageResponse } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private readonly baseUrl = '/api/v1/accounts';

  constructor(private http: HttpClient) {}

  getAll(customerId?: string, status?: string, page = 0, size = 20): Observable<PageResponse<Account>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (customerId) params = params.set('customerId', customerId);
    if (status) params = params.set('status', status);

    return this.http.get<PageResponse<Account>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/${id}`);
  }

  open(req: OpenAccountRequest): Observable<Account> {
    return this.http.post<Account>(this.baseUrl, req);
  }

  updateStatus(id: string, status: 'ACTIVE' | 'BLOCKED'): Observable<Account> {
    return this.http.patch<Account>(`${this.baseUrl}/${id}/status`, { status });
  }

  close(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getMovements(accountId: string, page = 0, size = 20): Observable<PageResponse<Movement>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<Movement>>(`${this.baseUrl}/${accountId}/movements`, { params });
  }
}
