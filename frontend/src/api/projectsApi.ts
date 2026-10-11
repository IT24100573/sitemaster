import { api } from './client';
import { cleanParams } from '../lib/params';
import type { LookupOption, PageParams, PageResponse } from '../types/common';
import type {
  Milestone,
  MilestonePayload,
  Phase,
  PhasePayload,
  ProjectDetail,
  ProjectPayload,
  ProjectSummary,
  TeamMember,
} from '../types/project';

export const projectsApi = {
  list: async (page: PageParams, status?: string) =>
    (await api.get<PageResponse<ProjectSummary>>('/projects', { params: cleanParams({ ...page, status }) })).data,
  get: async (id: number) => (await api.get<ProjectDetail>(`/projects/${id}`)).data,
  create: async (payload: ProjectPayload) => (await api.post<ProjectDetail>('/projects', payload)).data,
  update: async (id: number, payload: ProjectPayload) =>
    (await api.put<ProjectDetail>(`/projects/${id}`, payload)).data,
  changeStatus: async (id: number, status: string) =>
    (await api.patch<ProjectDetail>(`/projects/${id}/status`, { status })).data,
  uploadCover: async (id: number, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return (await api.post<ProjectDetail>(`/projects/${id}/cover`, form)).data;
  },
  removeCover: async (id: number) => (await api.delete<ProjectDetail>(`/projects/${id}/cover`)).data,
  coverUrl: (id: number, version: number | string) => `/api/projects/${id}/cover?v=${version}`,

  members: async (id: number) => (await api.get<TeamMember[]>(`/projects/${id}/members`)).data,
  candidates: async (id: number) => (await api.get<LookupOption[]>(`/projects/${id}/members/candidates`)).data,
  addMember: async (id: number, userId: number) =>
    (await api.post<TeamMember>(`/projects/${id}/members`, { userId })).data,
  removeMember: async (id: number, userId: number) => {
    await api.delete(`/projects/${id}/members/${userId}`);
  },

  phases: async (id: number) => (await api.get<Phase[]>(`/projects/${id}/phases`)).data,
  createPhase: async (id: number, payload: PhasePayload) =>
    (await api.post<Phase>(`/projects/${id}/phases`, payload)).data,
  updatePhase: async (phaseId: number, payload: PhasePayload) =>
    (await api.put<Phase>(`/phases/${phaseId}`, payload)).data,
  changePhaseStatus: async (phaseId: number, status: string) =>
    (await api.patch<Phase>(`/phases/${phaseId}/status`, { status })).data,
  deletePhase: async (phaseId: number) => {
    await api.delete(`/phases/${phaseId}`);
  },

  milestones: async (id: number) => (await api.get<Milestone[]>(`/projects/${id}/milestones`)).data,
  createMilestone: async (id: number, payload: MilestonePayload) =>
    (await api.post<Milestone>(`/projects/${id}/milestones`, payload)).data,
  updateMilestone: async (milestoneId: number, payload: MilestonePayload) =>
    (await api.put<Milestone>(`/milestones/${milestoneId}`, payload)).data,
  completeMilestone: async (milestoneId: number, completed: boolean) =>
    (await api.patch<Milestone>(`/milestones/${milestoneId}/complete`, { completed })).data,
  deleteMilestone: async (milestoneId: number) => {
    await api.delete(`/milestones/${milestoneId}`);
  },
};
