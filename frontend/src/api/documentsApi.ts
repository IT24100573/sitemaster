import { api } from './client';
import { cleanParams } from '../lib/params';
import type { ProjectDocument } from '../types/document';

export const documentsApi = {
  list: async (projectId: number, category?: string, search?: string) =>
    (await api.get<ProjectDocument[]>(`/projects/${projectId}/documents`, { params: cleanParams({ category, search }) }))
      .data,
  create: async (projectId: number, title: string, category: string, description: string, file: File) => {
    const form = new FormData();
    form.append('title', title);
    form.append('category', category);
    if (description) {
      form.append('description', description);
    }
    form.append('file', file);
    return (await api.post<ProjectDocument>(`/projects/${projectId}/documents`, form)).data;
  },
  addVersion: async (documentId: number, file: File, notes: string) => {
    const form = new FormData();
    if (notes) {
      form.append('notes', notes);
    }
    form.append('file', file);
    return (await api.post<ProjectDocument>(`/documents/${documentId}/versions`, form)).data;
  },
  remove: async (documentId: number) => {
    await api.delete(`/documents/${documentId}`);
  },
  downloadUrl: (versionId: number) => `/api/document-versions/${versionId}/download`,
};
