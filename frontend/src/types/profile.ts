import type { RoleName } from './auth';

export interface Profile {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  role: RoleName;
  roleDisplayName: string;
  hasPhoto: boolean;
  lastLoginAt: string | null;
  employment: {
    employeeNo: string;
    employmentType: string;
    trade: string | null;
    joinDate: string;
  } | null;
}

export interface ProfilePayload {
  fullName: string;
  phone: string | null;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}
