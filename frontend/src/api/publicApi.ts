import { api } from './client';
import { ensureCsrfToken } from './authApi';
import type { MessageResponse } from '../types/api';
import type { LookupOption } from '../types/common';
import type { ProjectMapMarker, PublicProjectCard } from '../types/public';

export interface SupplierApplicationPayload {
  companyName: string;
  registrationNo: string;
  contactPerson: string;
  email: string;
  phone: string;
  address: string;
  materialCategories: string[];
  registrationDocument: File;
}

export const publicApi = {
  completedProjects: async () => (await api.get<PublicProjectCard[]>('/public/projects/completed')).data,
  mapMarkers: async () => (await api.get<ProjectMapMarker[]>('/public/projects/map')).data,
  coverUrl: (id: number) => `/api/public/projects/${id}/cover`,
  materialCategories: async () => (await api.get<LookupOption[]>('/public/lookups/material-categories')).data,
  applyAsSupplier: async (payload: SupplierApplicationPayload) => {
    await ensureCsrfToken();
    const form = new FormData();
    form.append('companyName', payload.companyName);
    form.append('registrationNo', payload.registrationNo);
    form.append('contactPerson', payload.contactPerson);
    form.append('email', payload.email);
    form.append('phone', payload.phone);
    form.append('address', payload.address);
    payload.materialCategories.forEach((category) => form.append('materialCategories', category));
    form.append('registrationDocument', payload.registrationDocument);
    return (await api.post<MessageResponse>('/public/supplier-applications', form)).data;
  },
};
