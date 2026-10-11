import { api } from './client';
import { cleanParams } from '../lib/params';
import type { PageParams, PageResponse } from '../types/common';
import type { EmployeeFilters, EmployeeListItem, EmployeePayload, EmployeeRecord } from '../types/employee';

export const employeesApi = {
  list: async (page: PageParams, filters: EmployeeFilters) =>
    (await api.get<PageResponse<EmployeeListItem>>('/employees', { params: cleanParams({ ...page, ...filters }) }))
      .data,
  get: async (id: number) => (await api.get<EmployeeRecord>(`/employees/${id}`)).data,
  create: async (payload: EmployeePayload) => (await api.post<EmployeeRecord>('/employees', payload)).data,
  update: async (id: number, payload: EmployeePayload) =>
    (await api.put<EmployeeRecord>(`/employees/${id}`, payload)).data,
  addDocument: async (id: number, documentType: string, title: string, file: File) => {
    const form = new FormData();
    form.append('documentType', documentType);
    form.append('title', title);
    form.append('file', file);
    return (await api.post<EmployeeRecord>(`/employees/${id}/documents`, form)).data;
  },
  removeDocument: async (id: number, documentId: number) =>
    (await api.delete<EmployeeRecord>(`/employees/${id}/documents/${documentId}`)).data,
  documentUrl: (id: number, documentId: number) => `/api/employees/${id}/documents/${documentId}/download`,
};
