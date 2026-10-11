import { api } from './client';
import type { LookupOption } from '../types/common';

async function get(path: string, params?: Record<string, string>): Promise<LookupOption[]> {
  const { data } = await api.get<LookupOption[]>(`/lookups/${path}`, { params });
  return data;
}

export const lookupsApi = {
  roles: () => get('roles'),
  employmentTypes: () => get('employment-types'),
  trades: () => get('trades'),
  modules: () => get('modules'),
  employeeDocumentTypes: () => get('employee-document-types'),
  unlinkedStaff: () => get('unlinked-staff'),
  users: (search: string) => get('users', search ? { search } : undefined),
  clients: () => get('clients'),
  projectManagers: () => get('project-managers'),
  materialCategories: () => get('material-categories'),
  materialUnits: () => get('material-units'),
  enumValues: (name: string) => get(`enum/${name}`),
};
