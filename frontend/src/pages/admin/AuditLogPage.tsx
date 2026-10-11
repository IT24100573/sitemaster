import { useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { auditApi } from '../../api/auditApi';
import { lookupsApi } from '../../api/lookupsApi';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import { useLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDateTime, humanize } from '../../lib/format';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { PageHeader } from '../../components/ui/PageHeader';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import type { AuditLogEntry, AuditLogFilters } from '../../types/audit';

/** Who did what and when, filterable by user, module and date. */
export default function AuditLogPage() {
  const table = useTableState('createdAt', 'desc');
  const [filters, setFilters] = useState<AuditLogFilters>({});
  const [userSearch, setUserSearch] = useState('');
  const debouncedUserSearch = useDebouncedValue(userSearch.trim());
  const modules = useLookup('modules');
  const users = useQuery({
    queryKey: ['lookup', 'users', debouncedUserSearch],
    queryFn: () => lookupsApi.users(debouncedUserSearch),
  });

  const rangeInvalid = Boolean(filters.from && filters.to && filters.to < filters.from);
  const logs = useQuery({
    queryKey: ['audit-logs', table.params, filters],
    queryFn: () => auditApi.list(table.params, filters),
    placeholderData: keepPreviousData,
    enabled: !rangeInvalid,
  });

  const setFilter = (key: keyof AuditLogFilters, value: string) => {
    setFilters((current) => ({ ...current, [key]: value || undefined }));
    table.resetPage();
  };

  const columns: Column<AuditLogEntry>[] = [
    { key: 'when', header: 'When', sortKey: 'createdAt', className: 'whitespace-nowrap', render: (e) => formatDateTime(e.createdAt) },
    { key: 'who', header: 'User', sortKey: 'actorName', render: (e) => e.actorName },
    { key: 'module', header: 'Module', sortKey: 'module', render: (e) => humanize(e.module) },
    { key: 'action', header: 'Action', sortKey: 'action', render: (e) => e.actionLabel },
    { key: 'description', header: 'Details', render: (e) => <span className="text-muted">{e.description}</span> },
  ];

  return (
    <>
      <PageHeader title="Audit log" description="Every sign-in, change and hand-off, newest first." />

      <DataTable
        columns={columns}
        page={logs.data}
        rowKey={(e) => e.id}
        loading={logs.isFetching}
        error={
          rangeInvalid
            ? 'The "to" date must be on or after the "from" date.'
            : logs.isError
              ? toApiError(logs.error).message
              : null
        }
        emptyMessage="No activity matches these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <div className="grid w-full gap-3 md:grid-cols-2 xl:grid-cols-3">
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Search details" />
            <div className="flex gap-2">
              <input
                type="search"
                value={userSearch}
                maxLength={100}
                onChange={(e) => setUserSearch(e.target.value)}
                placeholder="Find user"
                aria-label="Find user"
                className="form-input w-32"
              />
              <Select
                aria-label="Filter by user"
                options={users.data ?? []}
                placeholder="All users"
                value={filters.userId ?? ''}
                onChange={(e) => setFilter('userId', e.target.value)}
              />
            </div>
            <Select
              aria-label="Filter by module"
              options={modules.data ?? []}
              placeholder="All modules"
              value={filters.module ?? ''}
              onChange={(e) => setFilter('module', e.target.value)}
            />
            <label className="flex items-center gap-2 text-sm text-muted">
              From
              <input
                type="date"
                value={filters.from ?? ''}
                onChange={(e) => setFilter('from', e.target.value)}
                className="form-input"
              />
            </label>
            <label className="flex items-center gap-2 text-sm text-muted">
              To
              <input
                type="date"
                value={filters.to ?? ''}
                onChange={(e) => setFilter('to', e.target.value)}
                className={`form-input ${rangeInvalid ? 'form-input-invalid' : ''}`}
              />
            </label>
          </div>
        }
      />
    </>
  );
}
