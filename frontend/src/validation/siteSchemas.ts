import { z } from 'zod';
import { CLIENT_MESSAGES as MSG } from './messages';
import { optionalDate, PATTERNS, requiredDate, requiredMoney, todayIso } from './common';

export const QUANTITY = /^\d{1,11}(\.\d{1,3})?$/;

const quantity = z
  .string()
  .trim()
  .min(1, MSG.required)
  .refine((v) => QUANTITY.test(v) && Number(v) > 0, MSG.quantity);

function daysAgoIso(days: number): string {
  const d = new Date(`${todayIso()}T00:00:00`);
  d.setDate(d.getDate() - days);
  const offset = d.getTimezoneOffset() * 60_000;
  return new Date(d.getTime() - offset).toISOString().slice(0, 10);
}

export const REPORT_MIN_DATE = () => daysAgoIso(7);

export const dailyReportSchema = z.object({
  projectId: z.string().min(1, MSG.required),
  reportDate: requiredDate.refine((v) => v <= todayIso() && v >= daysAgoIso(7), MSG.reportDateRange),
  workDone: z.string().trim().min(1, MSG.required).min(10, MSG.workDone).max(2000, MSG.workDone),
  workerCount: z.string().trim().min(1, MSG.required).regex(/^\d{1,5}$/, MSG.workers),
  weather: z.string().min(1, MSG.required),
  problems: z.string().max(2000, MSG.textSize),
  materialsUsed: z.array(z.object({ materialId: z.string().min(1, MSG.required), quantity })).max(30),
});

export type DailyReportFormValues = z.infer<typeof dailyReportSchema>;

export const materialRequestSchema = z.object({
  projectId: z.string().min(1, MSG.required),
  phaseId: z.string().min(1, MSG.required),
  materialId: z.string().min(1, MSG.required),
  quantity,
  urgency: z.string().min(1, MSG.required),
  requiredBy: requiredDate.refine((v) => v >= todayIso(), MSG.requiredByPast),
  notes: z.string().max(500, MSG.noteSize),
});

export type MaterialRequestFormValues = z.infer<typeof materialRequestSchema>;

export const materialSchema = z.object({
  name: z.string().trim().min(1, MSG.required).min(2, MSG.materialName).max(100, MSG.materialName),
  category: z.string().min(1, MSG.required),
  unit: z.string().min(1, MSG.required),
  unitPrice: requiredMoney,
  description: z.string().max(500, MSG.noteSize),
});

export type MaterialFormValues = z.infer<typeof materialSchema>;

export const equipmentSchema = z.object({
  code: z.string().trim().min(1, MSG.required).regex(/^[A-Za-z0-9-]{2,30}$/, MSG.equipmentCode),
  name: z.string().trim().min(1, MSG.required).min(2, MSG.equipmentName).max(100, MSG.equipmentName),
  category: z.string().trim().min(1, MSG.required).min(2, MSG.equipmentCategory).max(50, MSG.equipmentCategory),
});

export type EquipmentFormValues = z.infer<typeof equipmentSchema>;

export const equipmentConditionSchema = z
  .object({
    status: z.string().min(1, MSG.required),
    notes: z.string().max(500, MSG.noteSize),
    lastServiceDate: optionalDate.refine((v) => v === '' || v <= todayIso(), MSG.dateFuture),
    nextServiceDate: optionalDate,
  })
  .refine((d) => !d.lastServiceDate || !d.nextServiceDate || d.nextServiceDate >= d.lastServiceDate, {
    message: MSG.dateRange,
    path: ['nextServiceDate'],
  });

export type EquipmentConditionFormValues = z.infer<typeof equipmentConditionSchema>;

export const paymentSchema = z
  .object({
    amount: requiredMoney.refine((v) => Number(v) > 0, MSG.moneyPositive),
    method: z.string().min(1, MSG.required),
    referenceNo: z.string().trim(),
    paymentDate: requiredDate.refine((v) => v <= todayIso(), MSG.dateFuture),
    notes: z.string().max(500, MSG.noteSize),
  })
  .refine((d) => d.method === 'CASH' || /^[A-Za-z0-9/-]{3,50}$/.test(d.referenceNo), {
    message: MSG.reference,
    path: ['referenceNo'],
  });

export type PaymentFormValues = z.infer<typeof paymentSchema>;

export const invoiceUploadSchema = z.object({
  invoiceNumber: z.string().trim().min(1, MSG.required).regex(/^[A-Za-z0-9/-]{1,50}$/, MSG.invoiceNumber),
  invoiceDate: requiredDate.refine((v) => v <= todayIso(), MSG.dateFuture),
  amount: requiredMoney.refine((v) => Number(v) > 0, MSG.moneyPositive),
});

export type InvoiceUploadFormValues = z.infer<typeof invoiceUploadSchema>;

export { PATTERNS };
