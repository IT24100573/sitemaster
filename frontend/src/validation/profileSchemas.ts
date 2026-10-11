import { z } from 'zod';
import { CLIENT_MESSAGES as MSG } from './messages';
import { optionalPhone, personName } from './common';
import { PASSWORD_RULES } from './authSchemas';

export const profileSchema = z.object({
  fullName: personName,
  phone: optionalPhone,
});

export const changePasswordSchema = z
  .object({
    currentPassword: z.string().min(1, MSG.required).max(64, MSG.passwordMax),
    newPassword: z
      .string()
      .min(1, MSG.required)
      .refine((value) => PASSWORD_RULES.every((rule) => rule.test(value)), MSG.passwordStrong),
    confirmPassword: z.string().min(1, MSG.required),
  })
  .refine((d) => d.newPassword === d.confirmPassword, { message: MSG.passwordMismatch, path: ['confirmPassword'] })
  .refine((d) => d.newPassword !== d.currentPassword, { message: MSG.passwordSame, path: ['newPassword'] });

export type ProfileFormValues = z.infer<typeof profileSchema>;
export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>;
