import { NavLink } from 'react-router-dom';
import { X } from 'lucide-react';
import { useCurrentUser } from '../../context/AuthContext';
import { navigationFor } from '../../config/navigation';
import { Brand } from './Brand';

interface SidebarProps {
  open: boolean;
  onClose: () => void;
}

/** White sidebar; the active item gets the orange tint and an orange left border. */
export function Sidebar({ open, onClose }: SidebarProps) {
  const user = useCurrentUser();
  const sections = navigationFor(user.role, user.permissions);

  return (
    <>
      {open && <div className="fixed inset-0 z-30 bg-ink/40 lg:hidden" onClick={onClose} aria-hidden />}
      <aside
        className={`fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r border-line bg-white transition-transform lg:translate-x-0 ${
          open ? 'translate-x-0' : '-translate-x-full'
        }`}
        aria-label="Main navigation"
      >
        <div className="hazard-band h-1.5 shrink-0" aria-hidden />
        <div className="flex h-16 items-center justify-between gap-2 px-5">
          <Brand showCompany />
          <button
            type="button"
            onClick={onClose}
            className="rounded-md p-1.5 text-muted hover:bg-surface hover:text-ink lg:hidden"
            aria-label="Close menu"
          >
            <X className="h-5 w-5" aria-hidden />
          </button>
        </div>

        <nav className="flex-1 space-y-6 overflow-y-auto px-3 py-4">
          {sections.map((section, index) => (
            <div key={section.title ?? index}>
              {section.title && <p className="mb-2 px-3 text-xs font-medium text-muted">{section.title}</p>}
              <ul className="space-y-1">
                {section.items.map(({ label, path, icon: Icon }) => (
                  <li key={path}>
                    <NavLink
                      to={path}
                      onClick={onClose}
                      className={({ isActive }) =>
                        `flex items-center gap-3 rounded-r-lg border-l-4 px-3 py-2.5 text-sm font-medium transition-colors ${
                          isActive
                            ? 'border-primary bg-primary-light text-primary-dark'
                            : 'border-transparent text-muted hover:bg-surface hover:text-ink'
                        }`
                      }
                    >
                      <Icon className="h-[18px] w-[18px]" aria-hidden />
                      {label}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>

        <div className="border-t border-line px-5 py-4 text-xs text-muted">
          Signed in as <span className="font-medium text-ink">{user.roleDisplayName}</span>
        </div>
      </aside>
    </>
  );
}
