import type { RoleName } from './auth';

export type EmploymentType = 'PERMANENT' | 'CONTRACT' | 'DAILY_WAGE';

export interface EmployeeListItem {
  id: number;
  employeeNo: string;
  userId: number;
  fullName: string;
  email: string;
  roleDisplayName: string;
  employmentType: EmploymentType;
  trade: string | null;
  joinDate: string;
  active: boolean;
}

export interface EmployeeDocument {
  id: number;
  documentType: string;
  title: string;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedAt: string;
}

export interface EmployeeRecord {
  id: number;
  employeeNo: string;
  userId: number;
  fullName: string;
  email: string;
  phone: string | null;
  role: RoleName;
  roleDisplayName: string;
  active: boolean;
  hasPhoto: boolean;
  nic: string;
  dateOfBirth: string;
  age: number;
  address: string;
  joinDate: string;
  employmentType: EmploymentType;
  trade: string | null;
  dailyRate: number | null;
  monthlySalary: number | null;
  contractAmount: number | null;
  contractStartDate: string | null;
  contractEndDate: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  documents: EmployeeDocument[];
  createdAt: string;
}

/** Body for create/update. Money values are sent as strings to keep exact decimals. */
export interface EmployeePayload {
  existingUserId?: number | null;
  email?: string | null;
  fullName: string;
  phone: string;
  nic: string;
  dateOfBirth: string;
  address: string;
  joinDate: string;
  employmentType: EmploymentType;
  trade: string | null;
  dailyRate: string | null;
  monthlySalary: string | null;
  contractAmount: string | null;
  contractStartDate: string | null;
  contractEndDate: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
}

export interface EmployeeFilters {
  employmentType?: string;
  trade?: string;
  active?: string;
}
