import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BellOff, CheckCheck, ChevronLeft, ChevronRight } from 'lucide-react';
import { notificationsApi } from '../../api/notificationsApi';
import { useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { toApiError } from '../../lib/apiError';
import { formatDateTime } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { PageHeader } from '../../components/ui/PageHeader';
import { Spinner } from '../../components/ui/Spinner';
import type { NotificationItem } from '../../types/notification';

const PAGE_SIZE = 20;

export default function NotificationsPage() {
  const user = useCurrentUser();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [page, setPage] = useState(0);

  const list = useQuery({
    queryKey: ['notifications', 'page', page, unreadOnly],
    queryFn: () => notificationsApi.list({ page, size: PAGE_SIZE }, unreadOnly),
    placeholderData: keepPreviousData,
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['notifications'] });

  const markRead = useMutation({ mutationFn: notificationsApi.markRead, onSuccess: refresh });
  const markAll = useMutation({
    mutationFn: notificationsApi.markAllRead,
    onSuccess: (result) => {
      toast.success(result.message);
      refresh();
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const open = (item: NotificationItem) => {
    if (!item.read) {
      markRead.mutate(item.id);
    }
    if (item.link) {
      navigate(`/${user.rolePath}/${item.link}`);
    }
  };

  const data = list.data;

  return (
    <>
      <PageHeader
        title="Notifications"
        description="Hand-offs and changes that need your attention."
        actions={
          <Button variant="secondary" onClick={() => markAll.mutate()} loading={markAll.isPending}>
            <CheckCheck className="h-4 w-4" aria-hidden />
            Mark all as read
          </Button>
        }
      />

      <div className="mb-4 inline-flex rounded-lg border border-line bg-white p-1 text-sm" role="tablist">
        {[
          { label: 'All', value: false },
          { label: 'Unread', value: true },
        ].map((tab) => (
          <button
            key={tab.label}
            type="button"
            role="tab"
            aria-selected={unreadOnly === tab.value}
            onClick={() => {
              setUnreadOnly(tab.value);
              setPage(0);
            }}
            className={`rounded-md px-4 py-1.5 font-medium ${unreadOnly === tab.value ? 'bg-primary-light text-primary-dark' : 'text-muted hover:text-ink'}`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {list.isError && <Alert tone="error">{toApiError(list.error).message}</Alert>}

      <div className="card overflow-hidden">
        {list.isLoading ? (
          <div className="flex justify-center py-12">
            <Spinner className="h-6 w-6" />
          </div>
        ) : data && data.content.length > 0 ? (
          <ul className="divide-y divide-line">
            {data.content.map((item) => (
              <li key={item.id}>
                <button
                  type="button"
                  onClick={() => open(item)}
                  className={`flex w-full items-start gap-3 px-5 py-4 text-left hover:bg-surface ${item.read ? '' : 'bg-primary-light/50'}`}
                >
                  <span
                    className={`mt-1.5 h-2.5 w-2.5 shrink-0 rounded-full ${item.read ? 'bg-line' : 'bg-primary'}`}
                    aria-label={item.read ? 'Read' : 'Unread'}
                  />
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm font-medium text-ink">{item.title}</span>
                    <span className="mt-0.5 block text-sm text-muted">{item.message}</span>
                    <span className="mt-1 block text-xs text-muted">{formatDateTime(item.createdAt)}</span>
                  </span>
                </button>
              </li>
            ))}
          </ul>
        ) : (
          <div className="flex flex-col items-center gap-2 py-12 text-sm text-muted">
            <BellOff className="h-6 w-6" aria-hidden />
            {unreadOnly ? 'No unread notifications.' : 'No notifications yet.'}
          </div>
        )}

        {data && data.totalPages > 1 && (
          <div className="flex items-center justify-end gap-2 border-t border-line px-4 py-3 text-sm text-muted">
            <button type="button" disabled={page === 0} onClick={() => setPage(page - 1)} className="rounded-md p-1.5 hover:bg-surface disabled:opacity-40" aria-label="Previous page">
              <ChevronLeft className="h-4 w-4" aria-hidden />
            </button>
            Page {page + 1} of {data.totalPages}
            <button type="button" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)} className="rounded-md p-1.5 hover:bg-surface disabled:opacity-40" aria-label="Next page">
              <ChevronRight className="h-4 w-4" aria-hidden />
            </button>
          </div>
        )}
      </div>
    </>
  );
}
