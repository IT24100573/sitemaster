import { z } from 'zod';
import { CLIENT_MESSAGES as MSG } from './messages';
import { email, PATTERNS, requiredMoney, requiredPhone } from './common';

const days = z
  .string()
  .trim()
  .min(1, MSG.required)
  .refine((v) => /^\d{1,3}$/.test(v) && Number(v) <= 365, MSG.wholeDays);

const percent = z
  .string()
  .trim()
  .min(1, MSG.required)
  .refine((v) => PATTERNS.percent.test(v) && Number(v) <= 100, MSG.percent);

export const settingsSchema = z
  .object({
    companyName: z.string().trim().min(1, MSG.required).min(2, MSG.companyName).max(100, MSG.companyName),
    companyDescription: z.string().max(2000, MSG.textSize),
    companyAddress: z.string().trim().min(1, MSG.required).max(255, MSG.addressSize),
    companyPhone: requiredPhone,
    companyEmail: email,
    currency: z.string().trim().min(1, MSG.required).regex(PATTERNS.currency, MSG.currency),
    receiptThreshold: requiredMoney,
    siteAllowancePerDay: requiredMoney,
    workStartTime: z.string().min(1, MSG.required).regex(PATTERNS.time, MSG.required),
    workEndTime: z.string().min(1, MSG.required).regex(PATTERNS.time, MSG.required),
    holidays: z.array(z.string().regex(PATTERNS.isoDate, MSG.dateInvalid)).max(100),
    leaveAnnualDays: days,
    leaveCasualDays: days,
    leaveSickDays: days,
    epfEmployeeRate: percent,
    epfEmployerRate: percent,
    etfRate: percent,
    overtimeMultiplier: z
      .string()
      .trim()
      .min(1, MSG.required)
      .refine((v) => /^\d(\.\d{1,2})?$/.test(v) && Number(v) >= 1 && Number(v) <= 5, MSG.overtimeMultiplier),
    trades: z
      .array(z.string().trim().max(50, MSG.tradePattern).regex(PATTERNS.trade, MSG.tradePattern))
      .min(1, MSG.tradesRequired)
      .max(50),
    materialUnits: z
      .array(z.string().trim().regex(/^[\p{L}0-9 ./]{1,20}$/u, MSG.unitPattern))
      .min(1, MSG.required)
      .max(30),
    materialCategories: z
      .array(z.string().trim().max(50, MSG.tradePattern).regex(PATTERNS.trade, MSG.tradePattern))
      .min(1, MSG.categoriesRequired)
      .max(50),
  })
  .refine((d) => d.workStartTime < d.workEndTime, { message: MSG.workingHours, path: ['workEndTime'] });

export type SettingsFormValues = z.infer<typeof settingsSchema>;
