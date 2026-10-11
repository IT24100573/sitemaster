import { api } from './client';
import { cleanParams } from '../lib/params';
import type { PageParams, PageResponse } from '../types/common';
import type { BudgetCheck, MaterialRequest, MaterialRequestPayload } from '../types/materialRequest';
import type { PurchaseOrder } from '../types/purchase';

export const materialRequestsApi = {
  list: async (page: PageParams, filters: { projectId?: string; status?: string; mine?: boolean }) =>
    (await api.get<PageResponse<MaterialRequest>>('/material-requests', { params: cleanParams({ ...page, ...filters }) }))
      .data,
  get: async (id: number) => (await api.get<MaterialRequest>(`/material-requests/${id}`)).data,
  create: async (projectId: number, payload: MaterialRequestPayload) =>
    (await api.post<MaterialRequest>(`/projects/${projectId}/material-requests`, payload)).data,
  budgetCheck: async (id: number) => (await api.get<BudgetCheck>(`/material-requests/${id}/budget-check`)).data,
  approve: async (id: number, remarks: string | null, acknowledgeOverrun: boolean) =>
    (await api.post<MaterialRequest>(`/material-requests/${id}/approve`, { remarks, acknowledgeOverrun })).data,
  reject: async (id: number, remarks: string) =>
    (await api.post<MaterialRequest>(`/material-requests/${id}/reject`, { remarks })).data,
  confirmDelivery: async (id: number, deliveredQuantity: string, notes: string | null) =>
    (await api.post<MaterialRequest>(`/material-requests/${id}/delivery`, { deliveredQuantity, notes })).data,
  raisePurchaseOrder: async (id: number, supplierId: number, unitPrice: string, notes: string | null) =>
    (await api.post<PurchaseOrder>(`/material-requests/${id}/purchase-order`, { supplierId, unitPrice, notes })).data,
};
