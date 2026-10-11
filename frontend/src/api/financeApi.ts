import { api } from './client';
import { cleanParams } from '../lib/params';
import type { MessageResponse } from '../types/api';
import type { PageParams, PageResponse } from '../types/common';
import type { BudgetCheck } from '../types/materialRequest';
import type {
  ClientInvoice,
  ClientInvoicePayload,
  Dashboard,
  Expense,
  PettyCashFund,
  PettyCashTransaction,
  ProjectFinance,
  ReportInfo,
  ReportTable,
} from '../types/finance';

export interface ExpenseFormPayload {
  phaseId: string;
  category: string;
  amount: string;
  expenseDate: string;
  payee: string;
  description: string;
  receipt: File | null;
}

export const expensesApi = {
  list: async (page: PageParams, filters: { projectId?: string; status?: string; category?: string; mine?: boolean }) =>
    (await api.get<PageResponse<Expense>>('/expenses', { params: cleanParams({ ...page, ...filters }) })).data,
  get: async (id: number) => (await api.get<Expense>(`/expenses/${id}`)).data,
  create: async (projectId: number, payload: ExpenseFormPayload) => {
    const form = new FormData();
    form.append('phaseId', payload.phaseId);
    form.append('category', payload.category);
    form.append('amount', payload.amount);
    form.append('expenseDate', payload.expenseDate);
    form.append('payee', payload.payee);
    form.append('description', payload.description);
    if (payload.receipt) {
      form.append('receipt', payload.receipt);
    }
    return (await api.post<Expense>(`/projects/${projectId}/expenses`, form)).data;
  },
  budgetCheck: async (id: number) => (await api.get<BudgetCheck>(`/expenses/${id}/budget-check`)).data,
  approve: async (id: number, remarks: string | null, acknowledgeOverrun: boolean) =>
    (await api.post<Expense>(`/expenses/${id}/approve`, { remarks, acknowledgeOverrun })).data,
  reject: async (id: number, remarks: string) => (await api.post<Expense>(`/expenses/${id}/reject`, { remarks })).data,
  pay: async (id: number, payload: { fromPettyCash: boolean; method: string; referenceNo: string | null; paymentDate: string; notes: string | null }) =>
    (await api.post<Expense>(`/expenses/${id}/payment`, payload)).data,
  receiptUrl: (id: number) => `/api/expenses/${id}/receipt`,
};

export const pettyCashApi = {
  funds: async () => (await api.get<PettyCashFund[]>('/petty-cash')).data,
  fund: async (projectId: number) => (await api.get<PettyCashFund>(`/projects/${projectId}/petty-cash`)).data,
  save: async (projectId: number, payload: { custodianId: number; floatLimit: string; active: boolean }) =>
    (await api.put<PettyCashFund>(`/projects/${projectId}/petty-cash`, payload)).data,
  topUp: async (projectId: number, payload: { amount: string; date: string; description: string }) =>
    (await api.post<PettyCashFund>(`/projects/${projectId}/petty-cash/top-ups`, payload)).data,
  adjust: async (projectId: number, payload: { countedBalance: string; date: string; reason: string }) =>
    (await api.post<PettyCashFund>(`/projects/${projectId}/petty-cash/adjustments`, payload)).data,
  transactions: async (projectId: number, page: PageParams) =>
    (await api.get<PageResponse<PettyCashTransaction>>(`/projects/${projectId}/petty-cash/transactions`, { params: cleanParams({ ...page }) })).data,
};

export const clientInvoicesApi = {
  list: async (page: PageParams, filters: { projectId?: string; status?: string }) =>
    (await api.get<PageResponse<ClientInvoice>>('/client-invoices', { params: cleanParams({ ...page, ...filters }) })).data,
  get: async (id: number) => (await api.get<ClientInvoice>(`/client-invoices/${id}`)).data,
  create: async (payload: ClientInvoicePayload) => (await api.post<ClientInvoice>('/client-invoices', payload)).data,
  update: async (id: number, payload: ClientInvoicePayload) =>
    (await api.put<ClientInvoice>(`/client-invoices/${id}`, payload)).data,
  remove: async (id: number) => (await api.delete<MessageResponse>(`/client-invoices/${id}`)).data,
  issue: async (id: number) => (await api.post<ClientInvoice>(`/client-invoices/${id}/issue`)).data,
  receive: async (id: number, payload: { amount: string; paymentDate: string; method: string; referenceNo: string | null; notes: string | null }) =>
    (await api.post<ClientInvoice>(`/client-invoices/${id}/receipts`, payload)).data,
  pdfUrl: (id: number) => `/api/client-invoices/${id}/pdf`,
};

export const financeApi = {
  project: async (projectId: number) => (await api.get<ProjectFinance>(`/projects/${projectId}/finance`)).data,
  overview: async () => (await api.get<ProjectFinance[]>('/finance/projects')).data,
};

export interface ReportParams {
  from: string;
  to: string;
  projectId?: string;
}

export const reportsApi = {
  available: async () => (await api.get<ReportInfo[]>('/reports')).data,
  preview: async (type: string, params: ReportParams) =>
    (await api.get<ReportTable>(`/reports/${type}`, { params: cleanParams({ ...params }) })).data,
  exportUrl: (type: string, params: ReportParams, format: 'PDF' | 'XLSX' | 'CSV') => {
    const query = new URLSearchParams({ from: params.from, to: params.to, format });
    if (params.projectId) {
      query.set('projectId', params.projectId);
    }
    return `/api/reports/${type}/export?${query.toString()}`;
  },
};

export const dashboardApi = {
  get: async () => (await api.get<Dashboard>('/dashboard')).data,
};
