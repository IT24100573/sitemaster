import { api } from './client';
import type { PermissionMatrix, RoleWithPermissions } from '../types/role';

export const rolesApi = {
  matrix: async () => (await api.get<PermissionMatrix>('/admin/roles/matrix')).data,
  updatePermissions: async (roleId: number, permissionIds: number[]) =>
    (await api.put<RoleWithPermissions>(`/admin/roles/${roleId}/permissions`, { permissionIds })).data,
};
