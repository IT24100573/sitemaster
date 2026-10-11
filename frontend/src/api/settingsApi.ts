import { api } from './client';
import type { CompanyProfile, SystemSettings, SystemSettingsPayload } from '../types/settings';

export const settingsApi = {
  get: async () => (await api.get<SystemSettings>('/admin/settings')).data,
  update: async (payload: SystemSettingsPayload) => (await api.put<SystemSettings>('/admin/settings', payload)).data,
  uploadLogo: async (file: File) => {
    const form = new FormData();
    form.append('file', file);
    return (await api.post<SystemSettings>('/admin/settings/logo', form)).data;
  },
  removeLogo: async () => (await api.delete<SystemSettings>('/admin/settings/logo')).data,
};

export const companyApi = {
  profile: async () => (await api.get<CompanyProfile>('/public/company')).data,
  logoUrl: (version: number | string) => `/api/public/company/logo?v=${version}`,
};
