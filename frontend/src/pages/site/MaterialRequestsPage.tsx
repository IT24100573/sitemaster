import { useState } from 'react';
import { Link } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { materialRequestsApi } from '../../api/materialRequestsApi';
import { useAuth, useCurrentUser } from '../../context/AuthContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useEnumLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDate, formatMoney } from '../../lib/format';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { MaterialRequest } from '../../types/materialRequest';

/** Material requests across the user's projects (all projects for Accountant/Admin). */
export default function MaterialRequestsPage() {
  const user = useCurrentUser();
  const { hasPermission } = useAuth();
  const currency = useCurrency();
  const table = useTableState('createdAt', 'desc');
  const [projectId, setProjectId] = useState('');
  const [status, setStatus] = useState(user.role === 'ACCOUNTANT' ? 'APPROVED' : '');
  const [mine, setMine] = useState(user.role === 'SITE_ENGINEER');
  const statuses = useEnumLookup('request-statuses');

  const requests = useQuery({
    queryKey: ['material-requests', table.params, projectId, status, mine],
    queryFn: () => materialRequestsApi.list(table.params, { projectId: projectId || undefined, status: status || undefined, mine }),
    placeholderData: keepPreviousData,
  });

  const columns: Column<MaterialRequest>[] = [
    {
      key: 'material',
      header: 'Material',
      render: (r) => (
        <Link to={String(r.id)} className="block">
          <span className="font-medium text-ink hover:text-primary">{r.materialName}</span>
          <span className="block text-xs text-muted">
            {Number(r.quantity).toLocaleString('en-LK', { maximumFractionDigits: 3 })} {r.unit}, {r.projectName}
          </span>
        </Link>
      ),
    },
    { key: 'by', header: 'Requested by', render: (r) => r.requestedByName },
    { key: 'urgency', header: 'Urgency', sortKey: 'urgency', render: (r) => <StatusBadge status={r.urgency} /> },
    { key: 'needed', header: 'Needed by', sortKey: 'requiredBy', className: 'whitespace-nowrap', render: (r) => formatDate(r.requiredBy) },
    { key: 'cost', header: 'Estimate', className: 'whitespace-nowrap text-right', render: (r) => formatMoney(r.estimatedCost, currency) },
    { key: 'status', header: 'Status', sortKey: 'status', render: (r) => <StatusBadge status={r.status} /> },
  ];

  return (
    <>
      <PageHeader
        title="Material requests"
        description={user.role === 'ACCOUNTANT' ? 'Approved requests waiting for a purchase order are shown first.' : 'Requests for materials from your sites.'}
        actions={
          hasPermission('MATERIAL_REQUEST:CREATE') && user.role === 'SITE_ENGINEER' && (
            <Link to="new" className="inline-flex h-11 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-white hover:bg-primary-dark">
              <Plus className="h-4 w-4" aria-hidden />
              Request material
            </Link>
          )
        }
      />
      <DataTable
        columns={columns}
        page={requests.data}
        rowKey={(r) => r.id}
        loading={requests.isFetching}
        error={requests.isError ? toApiError(requests.error).message : null}
        emptyMessage="No requests match these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Search materials" />
            <div className="flex flex-wrap items-center gap-2 sm:ml-auto">
              {user.role === 'SITE_ENGINEER' && (
                <label className="flex items-center gap-2 text-sm text-muted">
                  <input type="checkbox" className="accent-primary" checked={mine} onChange={(e) => { setMine(e.target.checked); table.resetPage(); }} />
                  Mine only
                </label>
              )}
              <ProjectSelect aria-label="Filter by project" value={projectId} onChange={(e) => { setProjectId(e.target.value); table.resetPage(); }} />
              <Select aria-label="Filter by status" options={statuses.data ?? []} placeholder="Any status" value={status} onChange={(e) => { setStatus(e.target.value); table.resetPage(); }} />
            </div>
          </>
        }
      />
    </>
  );
}
