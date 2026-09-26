import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Customer, PageResponse, RegisterCustomerRequest, UpdateCustomerRequest } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class CustomerService {
  private readonly baseUrl = '/api/v1/customers';

  constructor(private http: HttpClient) {}

  getAll(page = 0, size = 20): Observable<PageResponse<Customer>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<Customer>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<Customer> {
    return this.http.get<Customer>(`${this.baseUrl}/${id}`);
  }

  create(customer: RegisterCustomerRequest): Observable<Customer> {
    return this.http.post<Customer>(this.baseUrl, customer);
  }

  update(id: string, customer: UpdateCustomerRequest): Observable<Customer> {
    return this.http.put<Customer>(`${this.baseUrl}/${id}`, customer);
  }

  deactivate(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}
