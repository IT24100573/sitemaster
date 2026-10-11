import { z } from 'zod';
import { CLIENT_MESSAGES as MSG } from './messages';

/** Mirrors the server @StrongPassword rule. */
export const PASSWORD_RULES = [
  { label: '8 to 64 characters', test: (v: string) => v.length >= 8 && v.length <= 64 },
  { label: 'An uppercase letter', test: (v: string) => /[A-Z]/.test(v) },
  { label: 'A lowercase letter', test: (v: string) => /[a-z]/.test(v) },
  { label: 'A number', test: (v: string) => /\d/.test(v) },
  { label: 'A special character', test: (v: string) => /[^A-Za-z0-9\s]/.test(v) },
] as const;

const email = z
  .string()
  .trim()
  .min(1, MSG.required)
  .max(100, MSG.emailMax)
  .email(MSG.emailInvalid);

const strongPassword = z
  .string()
  .min(1, MSG.required)
  .refine((value) => PASSWORD_RULES.every((rule) => rule.test(value)), MSG.passwordStrong);

export const loginSchema = z.object({
  email,
  password: z.string().min(1, MSG.required).max(64, MSG.passwordMax),
});

export const forgotPasswordSchema = z.object({ email });

export const resetPasswordSchema = z
  .object({
    newPassword: strongPassword,
    confirmPassword: z.string().min(1, MSG.required),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: MSG.passwordMismatch,
    path: ['confirmPassword'],
  });

export type LoginFormValues = z.infer<typeof loginSchema>;
export type ForgotPasswordFormValues = z.infer<typeof forgotPasswordSchema>;
export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>;
