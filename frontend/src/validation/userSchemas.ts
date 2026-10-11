import { z } from 'zod';
import { CLIENT_MESSAGES as MSG } from './messages';
import { email, optionalPhone, personName } from './common';

export const userSchema = z.object({
  fullName: personName,
  email,
  phone: optionalPhone,
  role: z.string().min(1, MSG.required),
});

export type UserFormValues = z.infer<typeof userSchema>;
