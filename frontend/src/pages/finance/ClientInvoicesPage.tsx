import { useState } from 'react';
import { Link } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { clientInvoicesApi } from '../../api/financeApi';
import { useAuth } from '../../context/AuthContext';
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
import type { ClientInvoice } from '../../types/finance';

/** Invoices to clients and what they still owe. */
export default function ClientInvoicesPage() {
  const { hasPermission } = useAuth();
  const currency = useCurrency();
  const table = useTableState('createdAt', 'desc');
  const [projectId, setProjectId] = useState('');
  const [status, setStatus] = useState('');
  const statuses = useEnumLookup('invoice-statuses');

  const invoices = useQuery({
    queryKey: ['client-invoices', table.params, projectId, status],
    queryFn: () => clientInvoicesApi.list(table.params, { projectId: projectId || undefined, status: status || undefined }),
    placeholderData: keepPreviousData,
  });

  const columns: Column<ClientInvoice>[] = [
    { key: 'number', header: 'Invoice', sortKey: 'invoiceNumber', render: (i) => (
      <Link to={String(i.id)} className="block">
        <span className="font-medium text-ink hover:text-primary">{i.invoiceNumber}</span>
        <span className="block text-xs text-muted">{i.clientName}</span>
      </Link>
    ) },
    { key: 'project', header: 'Project', render: (i) => i.projectName },
    { key: 'issue', header: 'Date', sortKey: 'issueDate', className: 'whitespace-nowrap', render: (i) => formatDate(i.issueDate) },
    { key: 'due', header: 'Due', sortKey: 'dueDate', className: 'whitespace-nowrap', render: (i) => formatDate(i.dueDate) },
    { key: 'total', header: 'Total', sortKey: 'total', className: 'whitespace-nowrap text-right', render: (i) => formatMoney(i.total, currency) },
    { key: 'owed', header: 'Owed', className: 'whitespace-nowrap text-right', render: (i) => formatMoney(i.outstanding, currency) },
    { key: 'status', header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
  ];

  return (
    <>
      <PageHeader title="Client invoices" description="Bill clients for project work and record what they pay."
        actions={hasPermission('INVOICE:CREATE') && (
          <Link to="new" className="inline-flex h-11 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-white hover:bg-primary-dark">
            <Plus className="h-4 w-4" aria-hidden />New invoice
          </Link>
        )} />
      <DataTable
        columns={columns}
        page={invoices.data}
        rowKey={(i) => i.id}
        loading={invoices.isFetching}
        error={invoices.isError ? toApiError(invoices.error).message : null}
        emptyMessage="No invoices match these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Invoice number" />
            <div className="flex flex-wrap gap-2 sm:ml-auto">
              <ProjectSelect aria-label="Filter by project" value={projectId} onChange={(e) => { setProjectId(e.target.value); table.resetPage(); }} />
              <Select aria-label="Filter by status" options={statuses.data ?? []} placeholder="Any status" value={status} onChange={(e) => { setStatus(e.target.value); table.resetPage(); }} />
            </div>
          </>
        }
      />
    </>
  );
}
