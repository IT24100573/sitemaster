import { api } from './client';
import { cleanParams } from '../lib/params';
import type { PageParams, PageResponse } from '../types/common';
import type { Equipment, EquipmentConditionPayload, EquipmentPayload } from '../types/equipment';

export const equipmentApi = {
  list: async (page: PageParams, projectId?: string, status?: string) =>
    (await api.get<PageResponse<Equipment>>('/equipment', { params: cleanParams({ ...page, projectId, status }) })).data,
  create: async (payload: EquipmentPayload) => (await api.post<Equipment>('/equipment', payload)).data,
  update: async (id: number, payload: EquipmentPayload) => (await api.put<Equipment>(`/equipment/${id}`, payload)).data,
  assign: async (id: number, projectId: number | null) =>
    (await api.patch<Equipment>(`/equipment/${id}/assignment`, { projectId })).data,
  condition: async (id: number, payload: EquipmentConditionPayload) =>
    (await api.patch<Equipment>(`/equipment/${id}/condition`, payload)).data,
};
