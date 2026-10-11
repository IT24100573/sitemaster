import { z } from 'zod';
import { CLIENT_MESSAGES as MSG } from './messages';
import {
  email,
  optionalDate,
  optionalPersonName,
  PATTERNS,
  personName,
  requiredDate,
  requiredMoney,
  requiredPhone,
} from './common';

/** Up to 3 integer digits and 6 decimals, within +/- limit. */
const coordinate = (limit: number, message: string) =>
  z
    .string()
    .trim()
    .min(1, MSG.required)
    .refine((v) => /^-?\d{1,3}(\.\d{1,6})?$/.test(v) && Math.abs(Number(v)) <= limit, message);

export const projectSchema = z
  .object({
    name: z.string().trim().min(1, MSG.required).min(2, MSG.projectName).max(150, MSG.projectName),
    description: z.string().max(2000, MSG.textSize),
    clientId: z.string().min(1, MSG.required),
    managerId: z.string().min(1, MSG.required),
    location: z.string().trim().min(1, MSG.required).max(255, MSG.addressSize),
    latitude: coordinate(90, MSG.latitude),
    longitude: coordinate(180, MSG.longitude),
    startDate: requiredDate,
    endDate: requiredDate,
    budget: requiredMoney.refine((v) => Number(v) > 0, MSG.moneyPositive),
  })
  .refine((d) => !d.startDate || !d.endDate || d.endDate > d.startDate, {
    message: MSG.endAfterStart,
    path: ['endDate'],
  });

export type ProjectFormValues = z.infer<typeof projectSchema>;

export const phaseSchema = z
  .object({
    name: z.string().trim().min(1, MSG.required).min(2, MSG.phaseName).max(100, MSG.phaseName),
    description: z.string().max(2000, MSG.textSize),
    startDate: requiredDate,
    endDate: requiredDate,
    budget: requiredMoney,
    sortOrder: z.string().refine((v) => v === '' || (/^\d{1,3}$/.test(v)), MSG.sortOrder),
  })
  .refine((d) => d.endDate >= d.startDate, { message: MSG.dateRange, path: ['endDate'] });

export type PhaseFormValues = z.infer<typeof phaseSchema>;

export const milestoneSchema = z.object({
  name: z.string().trim().min(1, MSG.required).min(2, MSG.title150).max(150, MSG.title150),
  dueDate: requiredDate,
  phaseId: z.string(),
});

export type MilestoneFormValues = z.infer<typeof milestoneSchema>;

export const taskSchema = z.object({
  phaseId: z.string().min(1, MSG.required),
  title: z.string().trim().min(1, MSG.required).min(2, MSG.title150).max(150, MSG.title150),
  description: z.string().max(2000, MSG.textSize),
  assigneeId: z.string(),
  priority: z.string().min(1, MSG.required),
  dueDate: requiredDate,
});

export type TaskFormValues = z.infer<typeof taskSchema>;

export const subTaskSchema = z.object({
  title: z.string().trim().min(1, MSG.required).min(2, MSG.title150).max(150, MSG.title150),
  assigneeId: z.string(),
  dueDate: optionalDate,
});

export type SubTaskFormValues = z.infer<typeof subTaskSchema>;

export const issueSchema = z.object({
  type: z.string().min(1, MSG.required),
  title: z.string().trim().min(1, MSG.required).min(5, MSG.issueTitle).max(150, MSG.issueTitle),
  description: z.string().trim().min(1, MSG.required).min(10, MSG.issueDescription).max(2000, MSG.issueDescription),
  severity: z.string().min(1, MSG.required),
  assignedToId: z.string(),
  dueDate: optionalDate,
});

export type IssueFormValues = z.infer<typeof issueSchema>;

export const documentSchema = z.object({
  title: z.string().trim().min(1, MSG.required).min(2, MSG.title150).max(150, MSG.title150),
  category: z.string().min(1, MSG.required),
  description: z.string().max(500, MSG.noteSize),
});

export type ProjectDocumentFormValues = z.infer<typeof documentSchema>;

export const clientSchema = z.object({
  name: z.string().trim().min(1, MSG.required).min(2, MSG.orgName).max(150, MSG.orgName),
  contactPerson: optionalPersonName,
  email,
  phone: requiredPhone,
  address: z.string().trim().max(255, MSG.addressSize),
});

export type ClientFormValues = z.infer<typeof clientSchema>;

export const supplierApplicationSchema = z.object({
  companyName: z.string().trim().min(1, MSG.required).min(2, MSG.orgName).max(150, MSG.orgName),
  registrationNo: z.string().trim().min(1, MSG.required).regex(/^[A-Za-z0-9 /-]{3,50}$/, MSG.registrationNo),
  contactPerson: personName,
  email,
  phone: requiredPhone,
  address: z.string().trim().min(1, MSG.required).max(255, MSG.addressSize),
  materialCategories: z.array(z.string()).min(1, MSG.categoriesRequired),
});

export type SupplierApplicationFormValues = z.infer<typeof supplierApplicationSchema>;

export { PATTERNS };
