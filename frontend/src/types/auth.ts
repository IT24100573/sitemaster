export type RoleName =
  | 'ADMIN'
  | 'PROJECT_MANAGER'
  | 'SITE_ENGINEER'
  | 'WORKER'
  | 'HR_MANAGER'
  | 'ACCOUNTANT'
  | 'SUPPLIER';

export type RolePath = 'admin' | 'pm' | 'site' | 'worker' | 'hr' | 'accounts' | 'supplier';

/** Mirrors CurrentUserResponse on the server. */
export interface CurrentUser {
  id: number;
  fullName: string;
  email: string;
  role: RoleName;
  roleDisplayName: string;
  rolePath: RolePath;
  /** MODULE:ACTION authorities, e.g. MATERIAL_REQUEST:APPROVE */
  permissions: string[];
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface ForgotPasswordPayload {
  email: string;
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
  confirmPassword: string;
}
