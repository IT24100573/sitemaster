import { z } from 'zod';
import { CLIENT_MESSAGES as MSG } from './messages';
import { optionalDate, optionalMoney, requiredDate, todayIso } from './common';

const TIME = /^([01]\d|2[0-3]):[0-5]\d$/;
const optionalTime = z.string().refine((v) => v === '' || TIME.test(v), MSG.required);

function minutes(t: string): number {
  const [h, m] = t.split(':').map(Number);
  return h * 60 + m;
}

export const attendanceSchema = z
  .object({
    projectId: z.string().min(1, MSG.required),
    employeeId: z.string().min(1, MSG.required),
    date: requiredDate.refine((v) => v <= todayIso(), MSG.dateFuture),
    status: z.string().min(1, MSG.required),
    shiftId: z.string(),
    checkIn: optionalTime,
    checkOut: optionalTime,
    overtimeHours: z.string().trim().refine((v) => v === '' || (/^\d(\.\d{1,2})?$/.test(v) && Number(v) <= 8), MSG.overtime),
    overtimeReason: z.string().max(500, MSG.noteSize),
    notes: z.string().max(500, MSG.noteSize),
  })
  .superRefine((d, ctx) => {
    if (d.checkOut && !d.checkIn) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['checkIn'], message: MSG.checkInFirst });
    }
    if (d.checkIn && d.checkOut) {
      const diff = minutes(d.checkOut) - minutes(d.checkIn);
      if (diff <= 0) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['checkOut'], message: MSG.timeOrder });
      } else if (diff > 16 * 60) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['checkOut'], message: MSG.maxHours });
      }
    }
    const working = d.status === 'PRESENT' || d.status === 'HALF_DAY';
    if (!working && d.overtimeHours !== '' && Number(d.overtimeHours) > 0) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, path: ['overtimeHours'], message: MSG.overtimeNotPresent });
    }
  });

export type AttendanceFormValues = z.infer<typeof attendanceSchema>;

export const leaveSchema = z
  .object({
    leaveTypeId: z.string().min(1, MSG.required),
    fromDate: requiredDate,
    toDate: requiredDate,
    reason: z.string().trim().min(1, MSG.required).min(5, MSG.reason).max(500, MSG.reason),
  })
  .refine((d) => !d.fromDate || !d.toDate || d.toDate >= d.fromDate, { message: MSG.dateRange, path: ['toDate'] })
  .refine((d) => !d.fromDate || !d.toDate || d.fromDate.slice(0, 4) === d.toDate.slice(0, 4), {
    message: MSG.leaveSameYear,
    path: ['toDate'],
  });

export type LeaveFormValues = z.infer<typeof leaveSchema>;

export const contractSchema = z
  .object({
    employeeId: z.string().min(1, MSG.required),
    contractNo: z.string().trim().min(1, MSG.required).regex(/^[A-Za-z0-9/-]{2,30}$/, MSG.contractNo),
    title: z.string().trim().min(1, MSG.required).min(2, MSG.title150).max(150, MSG.title150),
    startDate: requiredDate,
    endDate: requiredDate,
    contractValue: optionalMoney,
    notes: z.string().max(500, MSG.noteSize),
  })
  .refine((d) => !d.startDate || !d.endDate || d.endDate >= d.startDate, { message: MSG.dateRange, path: ['endDate'] });

export type ContractFormValues = z.infer<typeof contractSchema>;

export const trainingSchema = z
  .object({
    employeeId: z.string().min(1, MSG.required),
    title: z.string().trim().min(1, MSG.required).min(2, MSG.title150).max(150, MSG.title150),
    provider: z.string().max(150, MSG.title150),
    safety: z.boolean(),
    completedOn: requiredDate.refine((v) => v <= todayIso(), MSG.dateFuture),
    expiresOn: optionalDate,
    notes: z.string().max(500, MSG.noteSize),
  })
  .refine((d) => !d.expiresOn || d.expiresOn >= d.completedOn, { message: MSG.dateRange, path: ['expiresOn'] });

export type TrainingFormValues = z.infer<typeof trainingSchema>;

export const shiftSchema = z
  .object({
    name: z.string().trim().min(1, MSG.required).min(2, MSG.shiftName).max(50, MSG.shiftName),
    startTime: z.string().regex(TIME, MSG.required),
    endTime: z.string().regex(TIME, MSG.required),
    description: z.string().max(255, MSG.addressSize),
    active: z.boolean(),
  })
  .refine((d) => !TIME.test(d.startTime) || !TIME.test(d.endTime) || minutes(d.endTime) > minutes(d.startTime), {
    message: MSG.timeOrder,
    path: ['endTime'],
  });

export type ShiftFormValues = z.infer<typeof shiftSchema>;
