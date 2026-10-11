import { api } from './client';
import { cleanParams } from '../lib/params';
import type { MessageResponse } from '../types/api';
import type { PageParams, PageResponse } from '../types/common';
import type { UserPayload, UserRecord } from '../types/user';

export interface UserFilters {
  role?: string;
  active?: string;
}

export const usersApi = {
  list: async (page: PageParams, filters: UserFilters) =>
    (await api.get<PageResponse<UserRecord>>('/admin/users', { params: cleanParams({ ...page, ...filters }) })).data,
  get: async (id: number) => (await api.get<UserRecord>(`/admin/users/${id}`)).data,
  create: async (payload: UserPayload) => (await api.post<UserRecord>('/admin/users', payload)).data,
  update: async (id: number, payload: UserPayload) => (await api.put<UserRecord>(`/admin/users/${id}`, payload)).data,
  setActive: async (id: number, active: boolean) =>
    (await api.patch<UserRecord>(`/admin/users/${id}/status`, { active })).data,
  unlock: async (id: number) => (await api.post<UserRecord>(`/admin/users/${id}/unlock`)).data,
  sendPasswordLink: async (id: number) =>
    (await api.post<MessageResponse>(`/admin/users/${id}/password-link`)).data,
};
