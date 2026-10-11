import { api } from './client';
import { cleanParams } from '../lib/params';
import type { PageParams, PageResponse } from '../types/common';
import type { SupplierApplication, SupplierRecord } from '../types/supplier';

export const supplierApplicationsApi = {
  list: async (page: PageParams, status?: string) =>
    (
      await api.get<PageResponse<SupplierApplication>>('/admin/supplier-applications', {
        params: cleanParams({ ...page, status }),
      })
    ).data,
  get: async (id: number) => (await api.get<SupplierApplication>(`/admin/supplier-applications/${id}`)).data,
  approve: async (id: number, remarks: string | null) =>
    (await api.post<SupplierApplication>(`/admin/supplier-applications/${id}/approve`, { remarks })).data,
  reject: async (id: number, remarks: string) =>
    (await api.post<SupplierApplication>(`/admin/supplier-applications/${id}/reject`, { remarks })).data,
  documentUrl: (id: number) => `/api/admin/supplier-applications/${id}/document`,
};

export const suppliersApi = {
  list: async (page: PageParams, active?: string, category?: string) =>
    (await api.get<PageResponse<SupplierRecord>>('/suppliers', { params: cleanParams({ ...page, active, category }) }))
      .data,
  setActive: async (id: number, active: boolean) =>
    (await api.patch<SupplierRecord>(`/suppliers/${id}/status`, { active })).data,
};
