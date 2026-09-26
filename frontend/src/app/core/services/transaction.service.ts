import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  DepositRequest,
  PageResponse,
  Transaction,
  TransferRequest,
  WithdrawalRequest
} from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private readonly baseUrl = '/api/v1/transactions';

  constructor(private http: HttpClient) {}

  deposit(req: DepositRequest, idempotencyKey = crypto.randomUUID()): Observable<Transaction> {
    const headers = new HttpHeaders().set('Idempotency-Key', idempotencyKey);
    return this.http.post<Transaction>(`${this.baseUrl}/deposits`, req, { headers });
  }

  withdraw(req: WithdrawalRequest, idempotencyKey = crypto.randomUUID()): Observable<Transaction> {
    const headers = new HttpHeaders().set('Idempotency-Key', idempotencyKey);
    return this.http.post<Transaction>(`${this.baseUrl}/withdrawals`, req, { headers });
  }

  transfer(req: TransferRequest, idempotencyKey = crypto.randomUUID()): Observable<Transaction> {
    const headers = new HttpHeaders().set('Idempotency-Key', idempotencyKey);
    return this.http.post<Transaction>(`${this.baseUrl}/transfers`, req, { headers });
  }

  getById(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/${id}`);
  }

  search(filters: { accountId?: string; type?: string; status?: string; from?: string; to?: string } = {},
         page = 0, size = 20): Observable<PageResponse<Transaction>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filters.accountId) params = params.set('accountId', filters.accountId);
    if (filters.type) params = params.set('type', filters.type);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.from) params = params.set('from', filters.from);
    if (filters.to) params = params.set('to', filters.to);

    return this.http.get<PageResponse<Transaction>>(this.baseUrl, { params });
  }
}
