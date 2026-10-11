import { api } from './client';
import { cleanParams } from '../lib/params';
import type { PageParams, PageResponse } from '../types/common';
import type { DailyReport, DailyReportPayload, ReportFilters } from '../types/dailyReport';

export const dailyReportsApi = {
  list: async (page: PageParams, filters: ReportFilters) =>
    (await api.get<PageResponse<DailyReport>>('/daily-reports', { params: cleanParams({ ...page, ...filters }) })).data,
  get: async (id: number) => (await api.get<DailyReport>(`/daily-reports/${id}`)).data,
  submit: async (projectId: number, payload: DailyReportPayload) =>
    (await api.post<DailyReport>(`/projects/${projectId}/daily-reports`, payload)).data,
  update: async (id: number, payload: DailyReportPayload) =>
    (await api.put<DailyReport>(`/daily-reports/${id}`, payload)).data,
  addPhoto: async (id: number, file: File, caption: string) => {
    const form = new FormData();
    form.append('file', file);
    if (caption) {
      form.append('caption', caption);
    }
    return (await api.post<DailyReport>(`/daily-reports/${id}/photos`, form)).data;
  },
  removePhoto: async (id: number, photoId: number) =>
    (await api.delete<DailyReport>(`/daily-reports/${id}/photos/${photoId}`)).data,
  photoUrl: (photoId: number) => `/api/daily-report-photos/${photoId}`,
  approve: async (id: number, remarks: string | null) =>
    (await api.post<DailyReport>(`/daily-reports/${id}/approve`, { remarks })).data,
  reject: async (id: number, remarks: string) =>
    (await api.post<DailyReport>(`/daily-reports/${id}/reject`, { remarks })).data,
};
