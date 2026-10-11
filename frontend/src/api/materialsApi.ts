import { api } from './client';
import { cleanParams } from '../lib/params';
import type { LookupOption, PageParams, PageResponse } from '../types/common';
import type { Material, MaterialPayload, StockLine } from '../types/material';

export const materialsApi = {
  list: async (page: PageParams, category?: string, active?: string) =>
    (await api.get<PageResponse<Material>>('/materials', { params: cleanParams({ ...page, category, active }) })).data,
  options: async () => (await api.get<LookupOption[]>('/materials/options')).data,
  create: async (payload: MaterialPayload) => (await api.post<Material>('/materials', payload)).data,
  update: async (id: number, payload: MaterialPayload) => (await api.put<Material>(`/materials/${id}`, payload)).data,
  setActive: async (id: number, active: boolean) =>
    (await api.patch<Material>(`/materials/${id}/status`, { active })).data,
  stock: async (projectId: number) => (await api.get<StockLine[]>(`/projects/${projectId}/stock`)).data,
};
