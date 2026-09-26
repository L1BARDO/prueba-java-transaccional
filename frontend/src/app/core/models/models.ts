export type DocumentType = 'CC' | 'CE' | 'NIT' | 'PASSPORT';
export type CustomerStatus = 'ACTIVE' | 'INACTIVE';
export type AccountType = 'SAVINGS' | 'CHECKING';
export type AccountStatus = 'ACTIVE' | 'BLOCKED' | 'CLOSED';
export type CurrencyCode = 'COP' | 'USD';
export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER';
export type TransactionStatus = 'COMPLETED' | 'REJECTED';
export type MovementType = 'DEBIT' | 'CREDIT';

export interface Customer {
  id: string;
  documentType: DocumentType;
  documentNumber: string;
  fullName: string;
  email: string;
  phone?: string;
  status: CustomerStatus;
  createdAt: string;
  updatedAt: string;
  version?: number;
}

export interface RegisterCustomerRequest {
  documentType: DocumentType;
  documentNumber: string;
  fullName: string;
  email: string;
  phone?: string;
}

export interface UpdateCustomerRequest {
  fullName: string;
  email: string;
  phone?: string;
}

export interface Account {
  id: string;
  accountNumber: string;
  customerId: string;
  type: AccountType;
  currency: CurrencyCode;
  balance: number;
  status: AccountStatus;
  createdAt: string;
  updatedAt: string;
  version?: number;
}

export interface OpenAccountRequest {
  customerId: string;
  accountType: AccountType;
  currency: CurrencyCode;
}

export interface Movement {
  id: string;
  transactionId: string;
  accountId: string;
  type: MovementType;
  amount: number;
  currency: string;
  balanceAfter: number;
  createdAt: string;
}

export interface Transaction {
  id: string;
  reference: string;
  type: TransactionType;
  status: TransactionStatus;
  sourceAccountId?: string;
  destinationAccountId?: string;
  amount: number;
  currency: string;
  description?: string;
  failureCode?: string;
  failureReason?: string;
  createdAt: string;
}

export interface DepositRequest {
  accountId: string;
  amount: number;
  currency: CurrencyCode;
  description: string;
}

export interface WithdrawalRequest {
  accountId: string;
  amount: number;
  currency: CurrencyCode;
  description: string;
}

export interface TransferRequest {
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  currency: CurrencyCode;
  description: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tokenType?: string;
  expiresInSeconds?: number;
  userId?: string;
  username: string;
  fullName?: string;
  email?: string;
  customerId?: string | null;
  roles: string[];
  permissions: string[];
}

export interface UserSession {
  userId?: string;
  username: string;
  fullName?: string;
  email?: string;
  customerId?: string | null;
  token: string;
  roles: string[];
  permissions: string[];
}

export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  timestamp?: string;
  correlationId?: string;
  errors?: Record<string, string>;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
