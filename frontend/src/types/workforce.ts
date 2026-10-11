import type { LookupOption } from './common';

export interface AttendanceRecord {
  id: number;
  employeeId: number;
  employeeNo: string;
  employeeName: string;
  trade: string | null;
  projectId: number;
  projectName: string;
  shiftId: number | null;
  shiftName: string | null;
  date: string;
  status: 'PRESENT' | 'ABSENT' | 'HALF_DAY' | 'ON_LEAVE';
  checkIn: string | null;
  checkOut: string | null;
  hoursWorked: number | null;
  verified: boolean;
  verifiedByName: string | null;
  markedByName: string;
  notes: string | null;
  overtimeHours: number | null;
  overtimeStatus: string | null;
  locked: boolean;
  canVerify: boolean;
  canCorrect: boolean;
}

export interface TodayStatus {
  today: AttendanceRecord | null;
  projects: LookupOption[];
  onLeave: boolean;
}

export interface AttendancePayload {
  employeeId: number;
  projectId: number;
  date: string;
  status: string;
  shiftId: number | null;
  checkIn: string | null;
  checkOut: string | null;
  overtimeHours: string | null;
  overtimeReason: string | null;
  notes: string | null;
}

export interface SheetRow {
  employeeId: number;
  employeeNo: string;
  employeeName: string;
  days: Record<string, string>;
  present: number;
  halfDays: number;
  absent: number;
  onLeave: number;
  unverified: number;
}

export interface OvertimeRecord {
  id: number;
  attendanceId: number;
  employeeName: string;
  employeeNo: string;
  projectName: string;
  date: string;
  hours: number;
  reason: string | null;
  status: string;
  recordedByName: string;
  attendanceVerified: boolean;
  decidedByName: string | null;
  decidedAt: string | null;
  decisionNote: string | null;
}

export interface Shift {
  id: number;
  name: string;
  startTime: string;
  endTime: string;
  description: string | null;
  active: boolean;
}

export interface LeaveType {
  id: number;
  code: string;
  name: string;
  daysPerYear: number | null;
  entitlement: number;
  fromSettings: boolean;
  paid: boolean;
  backdateDays: number;
  active: boolean;
}

export interface LeaveBalance {
  leaveTypeId: number;
  leaveTypeName: string;
  paid: boolean;
  year: number;
  entitled: number;
  used: number;
  pending: number;
  available: number;
}

export interface LeaveRequestRecord {
  id: number;
  employeeId: number;
  employeeNo: string;
  employeeName: string;
  leaveTypeId: number;
  leaveTypeName: string;
  fromDate: string;
  toDate: string;
  days: number;
  reason: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  decidedByName: string | null;
  remarks: string | null;
  decidedAt: string | null;
  canDecide: boolean;
  canCancel: boolean;
  createdAt: string;
}

export interface Contract {
  id: number;
  employeeId: number;
  employeeNo: string;
  employeeName: string;
  contractNo: string;
  title: string;
  startDate: string;
  endDate: string;
  contractValue: number | null;
  notes: string | null;
  status: 'UPCOMING' | 'ACTIVE' | 'EXPIRED';
  daysToExpiry: number;
  expiringSoon: boolean;
  fileName: string | null;
}

export interface TrainingRecord {
  id: number;
  employeeId: number;
  employeeNo: string;
  employeeName: string;
  title: string;
  provider: string | null;
  safety: boolean;
  completedOn: string;
  expiresOn: string | null;
  expired: boolean;
  notes: string | null;
  fileName: string | null;
}

export interface SummaryRow {
  employeeId: number;
  employeeNo: string;
  employeeName: string;
  employmentType: string;
  presentDays: number;
  halfDays: number;
  absentDays: number;
  paidLeaveDays: number;
  unpaidLeaveDays: number;
  overtimeHours: number;
}

export interface PayrollPreview {
  year: number;
  month: number;
  workingDays: number;
  alreadySent: boolean;
  rows: SummaryRow[];
  unverifiedAttendance: number;
  pendingOvertime: number;
  pendingLeave: number;
}

export interface PayrollRun {
  id: number;
  year: number;
  month: number;
  status: 'SUMMARY_SENT' | 'CALCULATED' | 'PAID';
  workingDays: number;
  summarySentByName: string;
  summarySentAt: string;
  calculatedByName: string | null;
  calculatedAt: string | null;
  paidAt: string | null;
  totalGross: number;
  totalDeductions: number;
  totalNet: number;
  employees: number;
  unpaidPayslips: number;
}

export interface Payslip {
  id: number;
  payrollRunId: number;
  year: number;
  month: number;
  employeeId: number;
  employeeNo: string;
  employeeName: string;
  employmentType: string;
  grossPay: number;
  totalDeductions: number;
  netPay: number;
  paid: boolean;
  paidAt: string | null;
  paymentMethod: string | null;
  paymentReference: string | null;
  lines: { type: string; label: string; amount: number; runningTotal: number }[];
}

export interface PayrollRunDetail {
  run: PayrollRun;
  summaries: SummaryRow[];
  payslips: Payslip[];
}
