import { useState } from 'react';
import { Link } from 'react-router-dom';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { KeyRound, LockOpen, Pencil, Plus, Power } from 'lucide-react';
import { usersApi, type UserFilters } from '../../api/usersApi';
import { useAuth, useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDateTime } from '../../lib/format';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { PageHeader } from '../../components/ui/PageHeader';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { UserRecord } from '../../types/user';

type PendingAction = { kind: 'status' | 'unlock' | 'link'; user: UserRecord } | null;

const STATUS_OPTIONS = [
  { value: 'true', label: 'Active' },
  { value: 'false', label: 'Inactive' },
];

export default function UsersPage() {
  const me = useCurrentUser();
  const { hasPermission } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const table = useTableState('fullName');
  const [filters, setFilters] = useState<UserFilters>({});
  const [pending, setPending] = useState<PendingAction>(null);
  const roles = useLookup('roles');
  const canCreate = hasPermission('USER:CREATE');
  const canEdit = hasPermission('USER:EDIT');

  const users = useQuery({
    queryKey: ['users', table.params, filters],
    queryFn: () => usersApi.list(table.params, filters),
    placeholderData: keepPreviousData,
  });

  const action = useMutation({
    mutationFn: async (p: NonNullable<PendingAction>) => {
      if (p.kind === 'status') {
        await usersApi.setActive(p.user.id, !p.user.active);
        return p.user.active ? `${p.user.fullName} was deactivated.` : `${p.user.fullName} was activated.`;
      }
      if (p.kind === 'unlock') {
        await usersApi.unlock(p.user.id);
        return `${p.user.fullName} was unlocked.`;
      }
      return (await usersApi.sendPasswordLink(p.user.id)).message;
    },
    onSuccess: (message) => {
      toast.success(message);
      setPending(null);
      queryClient.invalidateQueries({ queryKey: ['users'] });
    },
    onError: (error) => {
      toast.error(toApiError(error).message);
      setPending(null);
    },
  });

  const setFilter = (key: keyof UserFilters, value: string) => {
    setFilters((current) => ({ ...current, [key]: value || undefined }));
    table.resetPage();
  };

  const columns: Column<UserRecord>[] = [
    {
      key: 'name',
      header: 'Name',
      sortKey: 'fullName',
      render: (u) => (
        <div>
          <p className="font-medium text-ink">{u.fullName}</p>
          <p className="text-xs text-muted">{u.email}</p>
        </div>
      ),
    },
    { key: 'role', header: 'Role', render: (u) => u.roleDisplayName },
    { key: 'phone', header: 'Phone', render: (u) => u.phone ?? '-' },
    {
      key: 'status',
      header: 'Status',
      render: (u) => (
        <div className="flex flex-wrap gap-1">
          <StatusBadge status={u.active ? 'ACTIVE' : 'INACTIVE'} />
          {u.locked && <StatusBadge status="REJECTED" label="Locked" />}
        </div>
      ),
    },
    { key: 'lastLogin', header: 'Last sign-in', sortKey: 'lastLoginAt', render: (u) => formatDateTime(u.lastLoginAt) },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (u) =>
        canEdit ? (
          <div className="flex justify-end gap-1">
            <Link
              to={`${u.id}/edit`}
              className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink"
              aria-label={`Edit ${u.fullName}`}
              title="Edit"
            >
              <Pencil className="h-4 w-4" aria-hidden />
            </Link>
            {u.locked && (
              <button
                type="button"
                onClick={() => setPending({ kind: 'unlock', user: u })}
                className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink"
                aria-label={`Unlock ${u.fullName}`}
                title="Unlock"
              >
                <LockOpen className="h-4 w-4" aria-hidden />
              </button>
            )}
            {u.active && (
              <button
                type="button"
                onClick={() => setPending({ kind: 'link', user: u })}
                className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink"
                aria-label={`Send password link to ${u.fullName}`}
                title="Send password link"
              >
                <KeyRound className="h-4 w-4" aria-hidden />
              </button>
            )}
            {u.id !== me.id && (
              <button
                type="button"
                onClick={() => setPending({ kind: 'status', user: u })}
                className={`rounded-md p-2 hover:bg-surface ${u.active ? 'text-red-600' : 'text-green-700'}`}
                aria-label={u.active ? `Deactivate ${u.fullName}` : `Activate ${u.fullName}`}
                title={u.active ? 'Deactivate' : 'Activate'}
              >
                <Power className="h-4 w-4" aria-hidden />
              </button>
            )}
          </div>
        ) : null,
    },
  ];

  const dialog = pending && {
    status: pending.user.active
      ? {
          title: `Deactivate ${pending.user.fullName}?`,
          message: 'They will be signed out at once and cannot sign in until the account is activated again.',
          confirm: 'Deactivate',
          tone: 'danger' as const,
        }
      : {
          title: `Activate ${pending.user.fullName}?`,
          message: 'They will be able to sign in again.',
          confirm: 'Activate',
          tone: 'primary' as const,
        },
    unlock: {
      title: `Unlock ${pending.user.fullName}?`,
      message: 'This clears the failed sign-in counter so they can try again now.',
      confirm: 'Unlock',
      tone: 'primary' as const,
    },
    link: {
      title: `Send a password link to ${pending.user.fullName}?`,
      message: 'Any earlier link stops working. On localhost the new link is written to the server console.',
      confirm: 'Send link',
      tone: 'primary' as const,
    },
  }[pending.kind];

  return (
    <>
      <PageHeader
        title="Users"
        description="Create staff accounts, change roles and control who can sign in."
        actions={
          canCreate && (
            <Link
              to="new"
              className="inline-flex h-11 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-white hover:bg-primary-dark"
            >
              <Plus className="h-4 w-4" aria-hidden />
              New user
            </Link>
          )
        }
      />

      <DataTable
        columns={columns}
        page={users.data}
        rowKey={(u) => u.id}
        loading={users.isFetching}
        error={users.isError ? toApiError(users.error).message : null}
        emptyMessage="No users match these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Search name or email" />
            <div className="flex gap-2 sm:ml-auto">
              <Select
                aria-label="Filter by role"
                options={roles.data ?? []}
                placeholder="All roles"
                value={filters.role ?? ''}
                onChange={(e) => setFilter('role', e.target.value)}
              />
              <Select
                aria-label="Filter by status"
                options={STATUS_OPTIONS}
                placeholder="Any status"
                value={filters.active ?? ''}
                onChange={(e) => setFilter('active', e.target.value)}
              />
            </div>
          </>
        }
      />

      {pending && dialog && (
        <ConfirmDialog
          open
          title={dialog.title}
          message={dialog.message}
          confirmLabel={dialog.confirm}
          tone={dialog.tone}
          loading={action.isPending}
          onConfirm={() => action.mutate(pending)}
          onCancel={() => setPending(null)}
        />
      )}
    </>
  );
}
