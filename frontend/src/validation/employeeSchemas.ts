import { z } from 'zod';
import { CLIENT_MESSAGES as MSG } from './messages';
import {
  ageFrom,
  nic,
  optionalDate,
  optionalMoney,
  optionalPersonName,
  optionalPhone,
  PATTERNS,
  personName,
  requiredDate,
  requiredPhone,
  todayIso,
} from './common';

/**
 * Employee form. All values are strings in the form; pay rules per employment
 * type mirror Employee.applyPayTerms on the server.
 */
export const employeeSchema = z
  .object({
    accountMode: z.enum(['new', 'existing']),
    existingUserId: z.string(),
    email: z.string().trim(),
    fullName: personName,
    phone: requiredPhone,
    nic,
    dateOfBirth: requiredDate,
    address: z.string().trim().min(1, MSG.required).max(255, MSG.addressSize),
    joinDate: requiredDate,
    employmentType: z.string().min(1, MSG.required),
    trade: z.string(),
    dailyRate: optionalMoney,
    monthlySalary: optionalMoney,
    contractAmount: optionalMoney,
    contractStartDate: optionalDate,
    contractEndDate: optionalDate,
    emergencyContactName: optionalPersonName,
    emergencyContactPhone: optionalPhone,
    isWorker: z.boolean(),
    isEdit: z.boolean(),
  })
  .superRefine((d, ctx) => {
    const issue = (path: string, message: string) => ctx.addIssue({ code: z.ZodIssueCode.custom, path: [path], message });

    if (!d.isEdit) {
      if (d.accountMode === 'existing' && d.existingUserId === '') {
        issue('existingUserId', MSG.accountChoice);
      }
      if (d.accountMode === 'new') {
        if (d.email === '') {
          issue('email', MSG.required);
        } else if (d.email.length > 100) {
          issue('email', MSG.emailMax);
        } else if (!PATTERNS.email.test(d.email)) {
          issue('email', MSG.emailInvalid);
        }
      }
    }
    if (PATTERNS.isoDate.test(d.dateOfBirth)) {
      const age = ageFrom(d.dateOfBirth);
      if (age < 18 || age > 65) {
        issue('dateOfBirth', MSG.age);
      }
    }
    if (PATTERNS.isoDate.test(d.joinDate) && d.joinDate > todayIso()) {
      issue('joinDate', MSG.dateFuture);
    }
    if (d.isWorker && d.trade === '') {
      issue('trade', MSG.tradeRequired);
    }
    const positive = (value: string, path: string) => {
      if (value === '') {
        issue(path, MSG.required);
      } else if (PATTERNS.money.test(value) && Number(value) <= 0) {
        issue(path, MSG.moneyPositive);
      }
    };
    if (d.employmentType === 'DAILY_WAGE') {
      positive(d.dailyRate, 'dailyRate');
    }
    if (d.employmentType === 'PERMANENT') {
      positive(d.monthlySalary, 'monthlySalary');
    }
    if (d.employmentType === 'CONTRACT') {
      positive(d.contractAmount, 'contractAmount');
      if (d.contractStartDate === '') {
        issue('contractStartDate', MSG.contractDates);
      }
      if (d.contractEndDate === '') {
        issue('contractEndDate', MSG.contractDates);
      }
      if (d.contractStartDate && d.contractEndDate && d.contractEndDate < d.contractStartDate) {
        issue('contractEndDate', MSG.dateRange);
      }
    }
  });

export type EmployeeFormValues = z.infer<typeof employeeSchema>;

export const documentSchema = z.object({
  documentType: z.string().min(1, MSG.required),
  title: z.string().trim().min(1, MSG.required).max(100, MSG.titleSize),
});

export type DocumentFormValues = z.infer<typeof documentSchema>;
