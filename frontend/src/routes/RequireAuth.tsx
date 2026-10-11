import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { FullPageSpinner } from '../components/ui/Spinner';
import type { RoleName } from '../types/auth';

/**
 * Sends signed-out users to /login and users of another role to /403.
 * This is for user experience only; the API enforces every rule itself.
 */
export function RequireAuth({ role, children }: { role?: RoleName; children: ReactNode }) {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <FullPageSpinner />;
  }
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (role && user.role !== role) {
    return <Navigate to="/403" replace />;
  }
  return <>{children}</>;
}
