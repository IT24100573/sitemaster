import { useState } from 'react';
import { Link } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { projectsApi } from '../../api/projectsApi';
import { useAuth, useCurrentUser } from '../../context/AuthContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useEnumLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDate, formatMoney } from '../../lib/format';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { ProjectSummary } from '../../types/project';

/** Projects the current user may see (all for Admin/Accountant, own team for others). */
export default function ProjectsPage() {
  const user = useCurrentUser();
  const { hasPermission } = useAuth();
  const currency = useCurrency();
  const table = useTableState('name');
  const [status, setStatus] = useState('');
  const statuses = useEnumLookup('project-statuses');
  const seesAll = user.role === 'ADMIN' || user.role === 'ACCOUNTANT';

  const projects = useQuery({
    queryKey: ['projects', table.params, status],
    queryFn: () => projectsApi.list(table.params, status || undefined),
    placeholderData: keepPreviousData,
  });

  const columns: Column<ProjectSummary>[] = [
    {
      key: 'name',
      header: 'Project',
      sortKey: 'name',
      render: (p) => (
        <Link to={String(p.id)} className="group block">
          <span className="font-medium text-ink group-hover:text-primary">{p.name}</span>
          <span className="block text-xs text-muted">
            {p.clientName}, {p.location}
          </span>
        </Link>
      ),
    },
    { key: 'pm', header: 'Project manager', render: (p) => p.managerName },
    { key: 'status', header: 'Status', sortKey: 'status', render: (p) => <StatusBadge status={p.status} /> },
    {
      key: 'progress',
      header: 'Progress',
      sortKey: 'progress',
      className: 'w-44',
      render: (p) => <ProgressBar value={p.progress} label={`${p.name} progress`} />,
    },
    {
      key: 'dates',
      header: 'Schedule',
      sortKey: 'endDate',
      className: 'whitespace-nowrap',
      render: (p) => `${formatDate(p.startDate)} - ${formatDate(p.endDate)}`,
    },
    { key: 'budget', header: 'Budget', className: 'whitespace-nowrap text-right', render: (p) => formatMoney(p.budget, currency) },
  ];

  return (
    <>
      <PageHeader
        title={seesAll ? 'Projects' : 'My projects'}
        description={seesAll ? 'Every project in the company.' : 'Projects you are assigned to.'}
        actions={
          hasPermission('PROJECT:CREATE') && (
            <Link to="new" className="inline-flex h-11 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-white hover:bg-primary-dark">
              <Plus className="h-4 w-4" aria-hidden />
              New project
            </Link>
          )
        }
      />
      <DataTable
        columns={columns}
        page={projects.data}
        rowKey={(p) => p.id}
        loading={projects.isFetching}
        error={projects.isError ? toApiError(projects.error).message : null}
        emptyMessage={seesAll ? 'No projects match these filters.' : 'You are not on any project yet.'}
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Name, client or location" />
            <div className="sm:ml-auto">
              <Select
                aria-label="Filter by status"
                options={statuses.data ?? []}
                placeholder="All statuses"
                value={status}
                onChange={(e) => {
                  setStatus(e.target.value);
                  table.resetPage();
                }}
              />
            </div>
          </>
        }
      />
    </>
  );
}
