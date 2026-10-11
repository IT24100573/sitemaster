export interface PermissionCell {
  id: number;
  module: string;
  action: string;
  authority: string;
}

export interface RoleWithPermissions {
  id: number;
  name: string;
  displayName: string;
  description: string | null;
  userCount: number;
  permissionIds: number[];
  lockedPermissionIds: number[];
}

export interface PermissionMatrix {
  modules: string[];
  actions: string[];
  permissions: PermissionCell[];
  roles: RoleWithPermissions[];
}
