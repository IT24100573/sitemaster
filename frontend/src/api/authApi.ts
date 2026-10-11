import axios from 'axios';
import { api } from './client';
import type { MessageResponse } from '../types/api';
import type {
  CurrentUser,
  ForgotPasswordPayload,
  LoginPayload,
  ResetPasswordPayload,
} from '../types/auth';

const CSRF_COOKIE = 'XSRF-TOKEN';

function hasCsrfCookie(): boolean {
  return document.cookie.split('; ').some((cookie) => cookie.startsWith(`${CSRF_COOKIE}=`));
}

/** The cookie is cleared on logout, so fetch a fresh one before the next POST. */
export async function ensureCsrfToken(): Promise<void> {
  if (!hasCsrfCookie()) {
    await api.get('/auth/csrf');
  }
}

/** Returns the signed-in user, or null when there is no valid session. */
export async function fetchCurrentUser(): Promise<CurrentUser | null> {
  try {
    const { data } = await api.get<CurrentUser>('/auth/me');
    return data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      return null;
    }
    throw error;
  }
}

export async function login(payload: LoginPayload): Promise<CurrentUser> {
  await ensureCsrfToken();
  const { data } = await api.post<CurrentUser>('/auth/login', payload);
  return data;
}

export async function logout(): Promise<void> {
  await ensureCsrfToken();
  await api.post('/auth/logout');
}

export async function requestPasswordReset(payload: ForgotPasswordPayload): Promise<MessageResponse> {
  await ensureCsrfToken();
  const { data } = await api.post<MessageResponse>('/auth/forgot-password', payload);
  return data;
}

export async function resetPassword(payload: ResetPasswordPayload): Promise<MessageResponse> {
  await ensureCsrfToken();
  const { data } = await api.post<MessageResponse>('/auth/reset-password', payload);
  return data;
}
