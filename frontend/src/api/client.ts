import axios from 'axios';

/**
 * Shared Axios instance.
 * - Same-origin through the Vite proxy, so the session cookie is sent automatically.
 * - Reads the XSRF-TOKEN cookie and sends it back as X-XSRF-TOKEN on every
 *   state-changing request (Spring Security CSRF protection).
 */
export const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
  withXSRFToken: true,
  timeout: 30_000,
  headers: { Accept: 'application/json' },
});

type UnauthorizedHandler = () => void;
let unauthorizedHandler: UnauthorizedHandler | null = null;

/** Registered by AuthProvider: called when a session expires mid-use. */
export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler;
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status: number | undefined = error?.response?.status;
    const url: string = error?.config?.url ?? '';
    // /auth/* calls handle their own 401s (wrong password, not signed in yet).
    if (status === 401 && !url.startsWith('/auth/')) {
      unauthorizedHandler?.();
    }
    return Promise.reject(error);
  },
);
