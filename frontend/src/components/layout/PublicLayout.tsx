import { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { Menu, X } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useCompanyProfile } from '../../hooks/useCompanyProfile';
import { dashboardPath } from '../../config/roles';
import { Brand } from './Brand';

const LINKS = [
  { to: '/', label: 'Home', end: true },
  { to: '/sites', label: 'Sites map', end: false },
  { to: '/about', label: 'About us', end: false },
  { to: '/become-a-supplier', label: 'Become a supplier', end: false },
];

/** Header and footer for the public website. */
export function PublicLayout() {
  const { user } = useAuth();
  const { data: company } = useCompanyProfile();
  const [open, setOpen] = useState(false);

  const cta = user ? (
    <Link to={dashboardPath(user)} className="inline-flex h-10 items-center rounded-lg bg-primary px-4 text-sm font-medium text-white hover:bg-primary-dark">
      Go to dashboard
    </Link>
  ) : (
    <Link to="/login" className="inline-flex h-10 items-center rounded-lg bg-primary px-4 text-sm font-medium text-white hover:bg-primary-dark">
      Sign in
    </Link>
  );

  return (
    <div className="flex min-h-screen flex-col bg-white">
      <div className="hazard-band h-1.5" aria-hidden />
      <header className="sticky top-0 z-30 border-b border-line bg-white/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-4 sm:px-6">
          <Link to="/" aria-label="Home">
            <Brand showCompany />
          </Link>
          <nav className="hidden items-center gap-1 md:flex" aria-label="Main">
            {LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                className={({ isActive }) =>
                  `rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'text-primary-dark' : 'text-muted hover:text-ink'}`
                }
              >
                {link.label}
              </NavLink>
            ))}
            <span className="ml-2">{cta}</span>
          </nav>
          <button
            type="button"
            onClick={() => setOpen((v) => !v)}
            className="rounded-md p-2 text-muted hover:bg-surface md:hidden"
            aria-label={open ? 'Close menu' : 'Open menu'}
            aria-expanded={open}
          >
            {open ? <X className="h-5 w-5" aria-hidden /> : <Menu className="h-5 w-5" aria-hidden />}
          </button>
        </div>
        {open && (
          <nav className="space-y-1 border-t border-line px-4 py-3 md:hidden" aria-label="Main">
            {LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                end={link.end}
                onClick={() => setOpen(false)}
                className={({ isActive }) =>
                  `block rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-primary-light text-primary-dark' : 'text-ink'}`
                }
              >
                {link.label}
              </NavLink>
            ))}
            <div className="pt-2">{cta}</div>
          </nav>
        )}
      </header>

      <main className="flex-1">
        <Outlet />
      </main>

      <footer className="border-t border-line bg-surface">
        <div className="mx-auto grid max-w-6xl gap-6 px-4 py-10 text-sm text-muted sm:grid-cols-3 sm:px-6">
          <div>
            <p className="font-semibold text-ink">{company?.name}</p>
            <p className="mt-1">{company?.address}</p>
          </div>
          <div>
            <p>{company?.phone}</p>
            <p>{company?.email}</p>
          </div>
          <p className="sm:text-right">
            &copy; {new Date().getFullYear()} {company?.name}
          </p>
        </div>
      </footer>
    </div>
  );
}
