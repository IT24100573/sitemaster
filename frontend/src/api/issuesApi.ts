import { api } from './client';
import { cleanParams } from '../lib/params';
import type { PageParams, PageResponse } from '../types/common';
import type { Issue, IssueFilters, IssuePayload } from '../types/issue';

export const issuesApi = {
  list: async (projectId: number, page: PageParams, filters: IssueFilters) =>
    (await api.get<PageResponse<Issue>>(`/projects/${projectId}/issues`, { params: cleanParams({ ...page, ...filters }) }))
      .data,
  report: async (projectId: number, payload: IssuePayload) =>
    (await api.post<Issue>(`/projects/${projectId}/issues`, payload)).data,
  update: async (issueId: number, payload: IssuePayload) => (await api.put<Issue>(`/issues/${issueId}`, payload)).data,
  changeStatus: async (issueId: number, status: string, note: string | null) =>
    (await api.patch<Issue>(`/issues/${issueId}/status`, { status, note })).data,
};
