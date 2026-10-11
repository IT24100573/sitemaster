import { z } from 'zod';
import { CLIENT_MESSAGES as MSG } from './messages';
import { requiredDate, requiredMoney, todayIso } from './common';
import { QUANTITY } from './siteSchemas';

const positiveMoney = requiredMoney.refine((v) => Number(v) > 0, MSG.moneyPositive);

export const expenseSchema = z.object({
  projectId: z.string().min(1, MSG.required),
  phaseId: z.string().min(1, MSG.required),
  category: z.string().min(1, MSG.required),
  amount: positiveMoney,
  expenseDate: requiredDate.refine((v) => v <= todayIso(), MSG.dateFuture),
  payee: z.string().trim().min(1, MSG.required).min(2, MSG.payee).max(100, MSG.payee),
  description: z.string().trim().min(1, MSG.required).min(5, MSG.expenseDescription).max(500, MSG.expenseDescription),
});

export type ExpenseFormValues = z.infer<typeof expenseSchema>;

export const invoiceSchema = z
  .object({
    projectId: z.string().min(1, MSG.required),
    issueDate: requiredDate,
    dueDate: requiredDate,
    taxRate: z.string().trim().refine((v) => /^\d{1,2}(\.\d{1,2})?$/.test(v) && Number(v) <= 30, MSG.taxRate),
    notes: z.string().max(500, MSG.noteSize),
    lines: z
      .array(
        z.object({
          description: z.string().trim().min(1, MSG.required).min(2, MSG.lineDescription).max(255, MSG.lineDescription),
          quantity: z.string().trim().refine((v) => QUANTITY.test(v) && Number(v) > 0, MSG.quantity),
          unitPrice: positiveMoney,
        }),
      )
      .min(1, MSG.linesRequired)
      .max(50),
  })
  .refine((d) => !d.issueDate || !d.dueDate || d.dueDate >= d.issueDate, { message: MSG.dueBeforeIssue, path: ['dueDate'] });

export type InvoiceFormValues = z.infer<typeof invoiceSchema>;
