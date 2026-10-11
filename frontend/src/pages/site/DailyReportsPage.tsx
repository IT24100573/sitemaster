import { useState } from 'react';
import { Link } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { dailyReportsApi } from '../../api/dailyReportsApi';
import { useAuth, useCurrentUser } from '../../context/AuthContext';
import { useEnumLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDate, humanize } from '../../lib/format';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { DailyReport, ReportFilters } from '../../types/dailyReport';

const REVIEW_STATUSES = ['PENDING', 'APPROVED', 'REJECTED'];

/** Daily site reports for the projects the user can see. */
export default function DailyReportsPage() {
  const user = useCurrentUser();
  const { hasPermission } = useAuth();
  const isEngineer = user.role === 'SITE_ENGINEER';
  const table = useTableState('reportDate', 'desc');
  const [filters, setFilters] = useState<ReportFilters>({ mine: isEngineer });
  const statuses = useEnumLookup('request-statuses');

  const reports = useQuery({
    queryKey: ['daily-reports', table.params, filters],
    queryFn: () => dailyReportsApi.list(table.params, filters),
    placeholderData: keepPreviousData,
  });

  const setFilter = (key: keyof ReportFilters, value: string) => {
    setFilters((current) => ({ ...current, [key]: value || undefined }));
    table.resetPage();
  };

  const columns: Column<DailyReport>[] = [
    {
      key: 'date',
      header: 'Date',
      sortKey: 'reportDate',
      className: 'whitespace-nowrap',
      render: (r) => (
        <Link to={String(r.id)} className="font-medium text-primary hover:text-primary-dark">
          {formatDate(r.reportDate)}
        </Link>
      ),
    },
    { key: 'project', header: 'Project', render: (r) => r.projectName },
    { key: 'by', header: 'Engineer', render: (r) => r.submittedByName },
    { key: 'work', header: 'Work done', render: (r) => <span className="line-clamp-2 text-muted">{r.workDone}</span> },
    { key: 'workers', header: 'Workers', render: (r) => r.workerCount },
    { key: 'weather', header: 'Weather', render: (r) => humanize(r.weather) },
    { key: 'status', header: 'Status', sortKey: 'status', render: (r) => <StatusBadge status={r.status} /> },
  ];

  return (
    <>
      <PageHeader
        title="Daily reports"
        description={isEngineer ? 'Your end-of-day reports. Rejected reports can be corrected and resubmitted.' : 'Reports from your sites.'}
        actions={
          hasPermission('DAILY_REPORT:CREATE') && isEngineer && (
            <Link to="new" className="inline-flex h-11 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-white hover:bg-primary-dark">
              <Plus className="h-4 w-4" aria-hidden />
              Submit report
            </Link>
          )
        }
      />
      <DataTable
        columns={columns}
        page={reports.data}
        rowKey={(r) => r.id}
        loading={reports.isFetching}
        error={reports.isError ? toApiError(reports.error).message : null}
        emptyMessage="No reports match these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Search work done" />
            <div className="flex flex-wrap gap-2 sm:ml-auto">
              <ProjectSelect aria-label="Filter by project" value={filters.projectId ?? ''} onChange={(e) => setFilter('projectId', e.target.value)} />
              <Select
                aria-label="Filter by status"
                options={(statuses.data ?? []).filter((s) => REVIEW_STATUSES.includes(s.value))}
                placeholder="Any status"
                value={filters.status ?? ''}
                onChange={(e) => setFilter('status', e.target.value)}
              />
              <input type="date" aria-label="From date" value={filters.from ?? ''} onChange={(e) => setFilter('from', e.target.value)} className="form-input w-auto" />
              <input type="date" aria-label="To date" value={filters.to ?? ''} onChange={(e) => setFilter('to', e.target.value)} className="form-input w-auto" />
            </div>
          </>
        }
      />
    </>
  );
}
