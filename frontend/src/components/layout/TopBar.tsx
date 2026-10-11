import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { LogOut, Menu } from 'lucide-react';
import { profileApi } from '../../api/profileApi';
import { useAuth, useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { Avatar } from '../ui/Avatar';
import { NotificationBell } from './NotificationBell';

export const PROFILE_KEY = ['profile'] as const;

export function TopBar({ onMenuClick }: { onMenuClick: () => void }) {
  const user = useCurrentUser();
  const { logout } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const [signingOut, setSigningOut] = useState(false);
  const profile = useQuery({ queryKey: PROFILE_KEY, queryFn: profileApi.get, staleTime: 5 * 60 * 1000 });

  const handleSignOut = async () => {
    setSigningOut(true);
    try {
      await logout();
      toast.success('Signed out.');
    } catch {
      toast.info('Signed out on this device. The server could not be reached to end the session.');
    } finally {
      setSigningOut(false);
      navigate('/login', { replace: true });
    }
  };

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-line bg-white px-4 lg:px-8">
      <button
        type="button"
        onClick={onMenuClick}
        className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink lg:hidden"
        aria-label="Open menu"
      >
        <Menu className="h-5 w-5" aria-hidden />
      </button>

      <div className="ml-auto flex items-center gap-2 sm:gap-3">
        <NotificationBell />
        <Link
          to={`/${user.rolePath}/profile`}
          className="flex items-center gap-3 rounded-lg px-1.5 py-1 hover:bg-surface"
          aria-label="Open your profile"
        >
          <div className="hidden text-right sm:block">
            <p className="text-sm font-medium leading-5 text-ink">{user.fullName}</p>
            <p className="text-xs text-muted">{user.roleDisplayName}</p>
          </div>
          <Avatar
            key={profile.dataUpdatedAt}
            userId={user.id}
            name={user.fullName}
            hasPhoto={profile.data?.hasPhoto ?? false}
            version={profile.dataUpdatedAt}
          />
        </Link>
        <button
          type="button"
          onClick={handleSignOut}
          disabled={signingOut}
          className="inline-flex h-9 items-center gap-2 rounded-lg px-3 text-sm font-medium text-muted hover:bg-surface hover:text-ink disabled:opacity-60"
        >
          <LogOut className="h-4 w-4" aria-hidden />
          <span className="hidden sm:inline">Sign out</span>
        </button>
      </div>
    </header>
  );
}
