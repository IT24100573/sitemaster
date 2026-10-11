import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { FullPageSpinner } from '../components/ui/Spinner';
import { dashboardPath, isOwnArea } from '../config/roles';

/**
 * For sign-in pages: once a user is signed in, send them back to the page they
 * originally asked for (if it is in their area) or to their dashboard.
 */
export function GuestOnly({ children }: { children: ReactNode }) {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return <FullPageSpinner />;
  }
  if (user) {
    const from = (location.state as { from?: string } | null)?.from;
    const target = from && isOwnArea(user, from) ? from : dashboardPath(user);
    return <Navigate to={target} replace />;
  }
  return <>{children}</>;
}
