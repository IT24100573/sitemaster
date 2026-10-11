import { z } from 'zod';
import { CLIENT_MESSAGES as MSG } from './messages';

/**
 * Field rules shared by every form. They mirror the server (ValidationPatterns.java
 * and the DTO annotations); the server remains the authority.
 */
export const PATTERNS = {
  personName: /^[\p{L} .-]+$/u,
  email: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  phone: /^(0\d{9}|\+94\d{9})$/,
  nic: /^(\d{9}[VvXx]|\d{12})$/,
  currency: /^[A-Z]{3}$/,
  trade: /^[\p{L} -]+$/u,
  money: /^\d{1,12}(\.\d{1,2})?$/,
  percent: /^\d{1,3}(\.\d{1,2})?$/,
  isoDate: /^\d{4}-\d{2}-\d{2}$/,
  time: /^([01]\d|2[0-3]):[0-5]\d$/,
};

export const personName = z
  .string()
  .trim()
  .min(1, MSG.required)
  .min(2, MSG.nameSize)
  .max(100, MSG.nameSize)
  .regex(PATTERNS.personName, MSG.namePattern);

export const optionalPersonName = z
  .string()
  .trim()
  .refine((v) => v === '' || (v.length >= 2 && v.length <= 100), MSG.nameSize)
  .refine((v) => v === '' || PATTERNS.personName.test(v), MSG.namePattern);

export const email = z.string().trim().min(1, MSG.required).max(100, MSG.emailMax).regex(PATTERNS.email, MSG.emailInvalid);

export const requiredPhone = z.string().trim().min(1, MSG.required).regex(PATTERNS.phone, MSG.phone);

export const optionalPhone = z
  .string()
  .trim()
  .refine((v) => v === '' || PATTERNS.phone.test(v), MSG.phone);

export const nic = z.string().trim().min(1, MSG.required).regex(PATTERNS.nic, MSG.nic);

export const requiredDate = z.string().min(1, MSG.required).regex(PATTERNS.isoDate, MSG.dateInvalid);

export const optionalDate = z.string().refine((v) => v === '' || PATTERNS.isoDate.test(v), MSG.dateInvalid);

export const optionalMoney = z
  .string()
  .trim()
  .refine((v) => v === '' || PATTERNS.money.test(v), MSG.money);

export const requiredMoney = z.string().trim().min(1, MSG.required).regex(PATTERNS.money, MSG.money);

/** Today as yyyy-MM-dd in the browser's local time. */
export function todayIso(): string {
  const now = new Date();
  const offset = now.getTimezoneOffset() * 60_000;
  return new Date(now.getTime() - offset).toISOString().slice(0, 10);
}

/** Whole years between an ISO date and today. */
export function ageFrom(isoDate: string): number {
  const [y, m, d] = isoDate.split('-').map(Number);
  const today = new Date();
  let age = today.getFullYear() - y;
  if (today.getMonth() + 1 < m || (today.getMonth() + 1 === m && today.getDate() < d)) {
    age -= 1;
  }
  return age;
}
