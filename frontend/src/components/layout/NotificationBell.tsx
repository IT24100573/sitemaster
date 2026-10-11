import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Bell } from 'lucide-react';
import { notificationsApi } from '../../api/notificationsApi';
import { useCurrentUser } from '../../context/AuthContext';
import { timeAgo } from '../../lib/format';
import { Spinner } from '../ui/Spinner';
import type { NotificationItem } from '../../types/notification';

const POLL_MS = 30_000;
const PREVIEW_SIZE = 5;

/** Bell with unread count (polled every 30 seconds) and a menu of the latest items. */
export function NotificationBell() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  const { data: unread = 0 } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: notificationsApi.unreadCount,
    refetchInterval: POLL_MS,
    refetchIntervalInBackground: false,
  });

  const latest = useQuery({
    queryKey: ['notifications', 'latest'],
    queryFn: () => notificationsApi.list({ page: 0, size: PREVIEW_SIZE }, false),
    enabled: open,
  });

  const markRead = useMutation({
    mutationFn: notificationsApi.markRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  });

  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const close = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    const onKey = (event: KeyboardEvent) => event.key === 'Escape' && setOpen(false);
    document.addEventListener('mousedown', close);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('mousedown', close);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  const openItem = (item: NotificationItem) => {
    if (!item.read) {
      markRead.mutate(item.id);
    }
    setOpen(false);
    if (item.link) {
      navigate(`/${user.rolePath}/${item.link}`);
    }
  };

  return (
    <div className="relative" ref={menuRef}>
      <button
        type="button"
        onClick={() => setOpen((value) => !value)}
        className="relative rounded-lg p-2 text-muted hover:bg-surface hover:text-ink"
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : 'Notifications'}
        aria-expanded={open}
      >
        <Bell className="h-5 w-5" aria-hidden />
        {unread > 0 && (
          <span className="absolute -right-0.5 -top-0.5 flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1 text-[11px] font-semibold text-white">
            {unread > 99 ? '99+' : unread}
          </span>
        )}
      </button>

      {open && (
        <div className="card absolute right-0 z-30 mt-2 w-[min(22rem,calc(100vw-2rem))] overflow-hidden">
          <div className="flex items-center justify-between border-b border-line px-4 py-3">
            <p className="text-sm font-semibold text-ink">Notifications</p>
            <span className="text-xs text-muted">{unread} unread</span>
          </div>
          {latest.isLoading ? (
            <div className="flex justify-center py-8">
              <Spinner />
            </div>
          ) : latest.data && latest.data.content.length > 0 ? (
            <ul className="max-h-96 divide-y divide-line overflow-y-auto">
              {latest.data.content.map((item) => (
                <li key={item.id}>
                  <button
                    type="button"
                    onClick={() => openItem(item)}
                    className={`block w-full px-4 py-3 text-left hover:bg-surface ${item.read ? '' : 'bg-primary-light/60'}`}
                  >
                    <p className="flex items-center gap-2 text-sm font-medium text-ink">
                      {!item.read && <span className="h-2 w-2 shrink-0 rounded-full bg-primary" aria-label="Unread" />}
                      {item.title}
                    </p>
                    <p className="mt-0.5 line-clamp-2 text-sm text-muted">{item.message}</p>
                    <p className="mt-1 text-xs text-muted">{timeAgo(item.createdAt)}</p>
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="px-4 py-8 text-center text-sm text-muted">You are all caught up.</p>
          )}
          <Link
            to={`/${user.rolePath}/notifications`}
            onClick={() => setOpen(false)}
            className="block border-t border-line px-4 py-3 text-center text-sm font-medium text-primary hover:bg-surface"
          >
            View all notifications
          </Link>
        </div>
      )}
    </div>
  );
}
