import type { ReactNode } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/** Sends users without a MODULE:ACTION permission to 403 (UX only; the API enforces it). */
export function RequirePermission({ permission, children }: { permission: string; children: ReactNode }) {
  const { hasPermission } = useAuth();
  if (!hasPermission(permission)) {
    return <Navigate to="/403" replace />;
  }
  return <>{children}</>;
}
