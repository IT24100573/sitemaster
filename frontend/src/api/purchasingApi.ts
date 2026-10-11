import { api } from './client';
import { cleanParams } from '../lib/params';
import type { PageParams, PageResponse } from '../types/common';
import type { PaymentPayload, PaymentRecord, PurchaseOrder, SupplierInvoice } from '../types/purchase';
import type { SupplierRecord } from '../types/supplier';

/** Staff endpoints (accountant, PM, site engineer, admin). */
export const purchasingApi = {
  orders: async (page: PageParams, filters: { projectId?: string; status?: string }) =>
    (await api.get<PageResponse<PurchaseOrder>>('/purchase-orders', { params: cleanParams({ ...page, ...filters }) }))
      .data,
  order: async (id: number) => (await api.get<PurchaseOrder>(`/purchase-orders/${id}`)).data,
  invoices: async (page: PageParams, status?: string) =>
    (await api.get<PageResponse<SupplierInvoice>>('/supplier-invoices', { params: cleanParams({ ...page, status }) }))
      .data,
  invoiceFileUrl: (id: number) => `/api/supplier-invoices/${id}/file`,
  pay: async (invoiceId: number, payload: PaymentPayload) =>
    (await api.post<SupplierInvoice>(`/supplier-invoices/${invoiceId}/payments`, payload)).data,
  payments: async (page: PageParams) =>
    (await api.get<PageResponse<PaymentRecord>>('/payments', { params: cleanParams({ ...page }) })).data,
};

/** Supplier portal endpoints: always the signed-in supplier's own data. */
export const supplierPortalApi = {
  orders: async (page: PageParams, status?: string) =>
    (await api.get<PageResponse<PurchaseOrder>>('/supplier/purchase-orders', { params: cleanParams({ ...page, status }) }))
      .data,
  order: async (id: number) => (await api.get<PurchaseOrder>(`/supplier/purchase-orders/${id}`)).data,
  accept: async (id: number) => (await api.post<PurchaseOrder>(`/supplier/purchase-orders/${id}/accept`)).data,
  decline: async (id: number, reason: string) =>
    (await api.post<PurchaseOrder>(`/supplier/purchase-orders/${id}/decline`, { reason })).data,
  dispatch: async (id: number, expectedDeliveryDate: string) =>
    (await api.post<PurchaseOrder>(`/supplier/purchase-orders/${id}/dispatch`, { expectedDeliveryDate })).data,
  uploadInvoice: async (id: number, invoiceNumber: string, invoiceDate: string, amount: string, file: File) => {
    const form = new FormData();
    form.append('invoiceNumber', invoiceNumber);
    form.append('invoiceDate', invoiceDate);
    form.append('amount', amount);
    form.append('file', file);
    return (await api.post<PurchaseOrder>(`/supplier/purchase-orders/${id}/invoice`, form)).data;
  },
  invoices: async (page: PageParams, status?: string) =>
    (await api.get<PageResponse<SupplierInvoice>>('/supplier/invoices', { params: cleanParams({ ...page, status }) }))
      .data,
  invoiceFileUrl: (id: number) => `/api/supplier/invoices/${id}/file`,
  payments: async (page: PageParams) =>
    (await api.get<PageResponse<PaymentRecord>>('/supplier/payments', { params: cleanParams({ ...page }) })).data,
  profile: async () => (await api.get<SupplierRecord>('/supplier/profile')).data,
  updateProfile: async (payload: { contactPerson: string; phone: string; address: string; materialCategories: string[] }) =>
    (await api.put<SupplierRecord>('/supplier/profile', payload)).data,
};
