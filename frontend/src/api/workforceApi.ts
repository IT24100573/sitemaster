import { api } from './client';
import { cleanParams } from '../lib/params';
import type { MessageResponse } from '../types/api';
import type { LookupOption, PageParams, PageResponse } from '../types/common';
import type {
  AttendancePayload,
  AttendanceRecord,
  Contract,
  LeaveBalance,
  LeaveRequestRecord,
  LeaveType,
  OvertimeRecord,
  PayrollPreview,
  PayrollRun,
  PayrollRunDetail,
  Payslip,
  SheetRow,
  Shift,
  TodayStatus,
  TrainingRecord,
} from '../types/workforce';

export const attendanceApi = {
  today: async () => (await api.get<TodayStatus>('/attendance/today')).data,
  checkIn: async (projectId: number) => (await api.post<AttendanceRecord>('/attendance/check-in', { projectId })).data,
  checkOut: async () => (await api.post<AttendanceRecord>('/attendance/check-out')).data,
  mine: async (year: number, month: number) =>
    (await api.get<AttendanceRecord[]>('/attendance/mine', { params: { year, month } })).data,
  team: async (projectId: number) => (await api.get<LookupOption[]>('/attendance/team', { params: { projectId } })).data,
  list: async (page: PageParams, filters: { projectId?: string; from?: string; to?: string; verified?: string }) =>
    (await api.get<PageResponse<AttendanceRecord>>('/attendance', { params: cleanParams({ ...page, ...filters }) })).data,
  mark: async (payload: AttendancePayload) => (await api.post<AttendanceRecord>('/attendance', payload)).data,
  correct: async (id: number, payload: AttendancePayload) =>
    (await api.put<AttendanceRecord>(`/attendance/${id}`, payload)).data,
  verify: async (id: number) => (await api.post<AttendanceRecord>(`/attendance/${id}/verify`)).data,
  sheet: async (year: number, month: number, projectId?: string) =>
    (await api.get<SheetRow[]>('/attendance/sheet', { params: cleanParams({ year, month, projectId }) })).data,
  overtime: async (page: PageParams, status?: string) =>
    (await api.get<PageResponse<OvertimeRecord>>('/overtime', { params: cleanParams({ ...page, status }) })).data,
  approveOvertime: async (id: number, remarks: string | null) =>
    (await api.post<OvertimeRecord>(`/overtime/${id}/approve`, { remarks })).data,
  rejectOvertime: async (id: number, remarks: string) =>
    (await api.post<OvertimeRecord>(`/overtime/${id}/reject`, { remarks })).data,
  shifts: async () => (await api.get<Shift[]>('/shifts')).data,
  saveShift: async (id: number | null, payload: Omit<Shift, 'id'>) =>
    id ? (await api.put<Shift>(`/shifts/${id}`, payload)).data : (await api.post<Shift>('/shifts', payload)).data,
};

export const leaveApi = {
  types: async (activeOnly = true) => (await api.get<LeaveType[]>('/leave/types', { params: { activeOnly } })).data,
  saveType: async (id: number | null, payload: Omit<LeaveType, 'id' | 'entitlement' | 'fromSettings'>) =>
    id ? (await api.put<LeaveType>(`/leave/types/${id}`, payload)).data
      : (await api.post<LeaveType>('/leave/types', payload)).data,
  myBalances: async (year: number) => (await api.get<LeaveBalance[]>('/leave/balances/mine', { params: { year } })).data,
  balances: async (employeeId: number, year: number) =>
    (await api.get<LeaveBalance[]>('/leave/balances', { params: { employeeId, year } })).data,
  apply: async (payload: { leaveTypeId: number; fromDate: string; toDate: string; reason: string }) =>
    (await api.post<LeaveRequestRecord>('/leave/requests', payload)).data,
  mine: async (page: PageParams) =>
    (await api.get<PageResponse<LeaveRequestRecord>>('/leave/requests/mine', { params: cleanParams({ ...page }) })).data,
  search: async (page: PageParams, status?: string) =>
    (await api.get<PageResponse<LeaveRequestRecord>>('/leave/requests', { params: cleanParams({ ...page, status }) })).data,
  approve: async (id: number, remarks: string | null) =>
    (await api.post<LeaveRequestRecord>(`/leave/requests/${id}/approve`, { remarks })).data,
  reject: async (id: number, remarks: string) =>
    (await api.post<LeaveRequestRecord>(`/leave/requests/${id}/reject`, { remarks })).data,
  cancel: async (id: number) => (await api.delete<MessageResponse>(`/leave/requests/${id}`)).data,
};

export interface ContractPayload {
  employeeId: number;
  contractNo: string;
  title: string;
  startDate: string;
  endDate: string;
  contractValue: string | null;
  notes: string | null;
}

export interface TrainingPayload {
  employeeId: number;
  title: string;
  provider: string | null;
  safety: boolean;
  completedOn: string;
  expiresOn: string | null;
  notes: string | null;
}

const upload = async <T>(url: string, file: File) => {
  const form = new FormData();
  form.append('file', file);
  return (await api.post<T>(url, form)).data;
};

export const hrRecordsApi = {
  contracts: async (page: PageParams, filters: { employeeId?: string; expiringWithinDays?: string }) =>
    (await api.get<PageResponse<Contract>>('/contracts', { params: cleanParams({ ...page, ...filters }) })).data,
  saveContract: async (id: number | null, payload: ContractPayload) =>
    id ? (await api.put<Contract>(`/contracts/${id}`, payload)).data : (await api.post<Contract>('/contracts', payload)).data,
  uploadContractFile: (id: number, file: File) => upload<Contract>(`/contracts/${id}/file`, file),
  contractFileUrl: (id: number) => `/api/contracts/${id}/file`,
  training: async (page: PageParams, filters: { employeeId?: string; safety?: string }) =>
    (await api.get<PageResponse<TrainingRecord>>('/training-records', { params: cleanParams({ ...page, ...filters }) })).data,
  saveTraining: async (id: number | null, payload: TrainingPayload) =>
    id ? (await api.put<TrainingRecord>(`/training-records/${id}`, payload)).data
      : (await api.post<TrainingRecord>('/training-records', payload)).data,
  uploadCertificate: (id: number, file: File) => upload<TrainingRecord>(`/training-records/${id}/certificate`, file),
  certificateUrl: (id: number) => `/api/training-records/${id}/certificate`,
  deleteTraining: async (id: number) => (await api.delete<MessageResponse>(`/training-records/${id}`)).data,
};

export const payrollApi = {
  preview: async (year: number, month: number) =>
    (await api.get<PayrollPreview>('/payroll/preview', { params: { year, month } })).data,
  sendSummary: async (year: number, month: number) => (await api.post<PayrollRun>('/payroll/runs', { year, month })).data,
  runs: async () => (await api.get<PayrollRun[]>('/payroll/runs')).data,
  run: async (id: number) => (await api.get<PayrollRunDetail>(`/payroll/runs/${id}`)).data,
  calculate: async (id: number) => (await api.post<PayrollRunDetail>(`/payroll/runs/${id}/calculate`)).data,
  payAll: async (id: number, method: string, reference: string | null) =>
    (await api.post<PayrollRunDetail>(`/payroll/runs/${id}/pay`, { method, reference })).data,
  paySlip: async (id: number, method: string, reference: string | null) =>
    (await api.post<Payslip>(`/payslips/${id}/pay`, { method, reference })).data,
  myPayslips: async () => (await api.get<Payslip[]>('/payslips/mine')).data,
  pdfUrl: (id: number) => `/api/payslips/${id}/pdf`,
};
