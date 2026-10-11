import type { RoleName } from './auth';

export interface UserRecord {
  id: number;
  fullName: string;
  email: string;
  phone: string | null;
  role: RoleName;
  roleDisplayName: string;
  active: boolean;
  locked: boolean;
  hasPhoto: boolean;
  lastLoginAt: string | null;
  createdAt: string;
}

export interface UserPayload {
  fullName: string;
  email: string;
  phone: string | null;
  role: RoleName;
}
