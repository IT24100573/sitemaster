import { api } from './client';
import { cleanParams } from '../lib/params';
import type { ClientPayload, ClientRecord } from '../types/client';
import type { PageParams, PageResponse } from '../types/common';

export const clientsApi = {
  list: async (page: PageParams, active?: string) =>
    (await api.get<PageResponse<ClientRecord>>('/clients', { params: cleanParams({ ...page, active }) })).data,
  create: async (payload: ClientPayload) => (await api.post<ClientRecord>('/clients', payload)).data,
  update: async (id: number, payload: ClientPayload) => (await api.put<ClientRecord>(`/clients/${id}`, payload)).data,
  setActive: async (id: number, active: boolean) =>
    (await api.patch<ClientRecord>(`/clients/${id}/status`, { active })).data,
};
