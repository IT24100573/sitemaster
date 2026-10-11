import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { dashboardPath } from '../../config/roles';

interface ErrorPageProps {
  code: string;
  title: string;
  message: string;
  icon: ReactNode;
  /** Inside the app shell the page should not take the whole screen. */
  embedded?: boolean;
}

/** Shared layout for 403 and 404, with a way back that fits the visitor. */
export function ErrorPage({ code, title, message, icon, embedded = false }: ErrorPageProps) {
  const { user } = useAuth();
  const target = user ? dashboardPath(user) : '/login';
  const label = user ? 'Go to your dashboard' : 'Go to sign in';

  return (
    <div className={`flex items-center justify-center p-6 ${embedded ? 'py-20' : 'min-h-screen bg-surface'}`}>
      <div className="card w-full max-w-md overflow-hidden text-center">
        <div className="hazard-band h-2" aria-hidden />
        <div className="p-8">
          <div className="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-primary-light text-primary">
            {icon}
          </div>
          <p className="mt-5 text-sm font-semibold text-primary">Error {code}</p>
          <h1 className="mt-1 text-2xl font-semibold tracking-tight text-ink">{title}</h1>
          <p className="mt-2 text-sm text-muted">{message}</p>
          <Link
            to={target}
            replace
            className="mt-6 inline-flex h-11 items-center rounded-lg bg-primary px-5 text-sm font-medium text-white hover:bg-primary-dark"
          >
            {label}
          </Link>
        </div>
      </div>
    </div>
  );
}
