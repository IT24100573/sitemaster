import { createContext, useCallback, useContext, useEffect, useMemo, type ReactNode } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { setUnauthorizedHandler } from '../api/client';
import { fetchCurrentUser, login as apiLogin, logout as apiLogout } from '../api/authApi';
import type { CurrentUser, LoginPayload } from '../types/auth';

export const CURRENT_USER_KEY = ['auth', 'me'] as const;

interface AuthContextValue {
  user: CurrentUser | null;
  isLoading: boolean;
  login: (payload: LoginPayload) => Promise<CurrentUser>;
  logout: () => Promise<void>;
  hasPermission: (authority: string) => boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: CURRENT_USER_KEY,
    queryFn: fetchCurrentUser,
    staleTime: 5 * 60 * 1000,
    retry: false,
  });

  const clearSession = useCallback(() => {
    queryClient.removeQueries({ predicate: (query) => query.queryKey[0] !== 'auth' });
    queryClient.setQueryData(CURRENT_USER_KEY, null);
  }, [queryClient]);

  // A 401 on any other call means the session expired: drop the user so the
  // route guard sends them back to the sign-in page.
  useEffect(() => {
    setUnauthorizedHandler(clearSession);
    return () => setUnauthorizedHandler(null);
  }, [clearSession]);

  const login = useCallback(
    async (payload: LoginPayload) => {
      const user = await apiLogin(payload);
      queryClient.setQueryData(CURRENT_USER_KEY, user);
      return user;
    },
    [queryClient],
  );

  const logout = useCallback(async () => {
    try {
      await apiLogout();
    } finally {
      clearSession();
    }
  }, [clearSession]);

  const user = data ?? null;

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isLoading,
      login,
      logout,
      hasPermission: (authority) => user?.permissions.includes(authority) ?? false,
    }),
    [user, isLoading, login, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider');
  }
  return context;
}

/** For pages behind RequireAuth, where a user is guaranteed. */
export function useCurrentUser(): CurrentUser {
  const { user } = useAuth();
  if (!user) {
    throw new Error('useCurrentUser called without a signed-in user');
  }
  return user;
}
