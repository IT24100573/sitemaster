import { useState } from 'react';
import { Link } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { supplierApplicationsApi } from '../../api/suppliersApi';
import { useEnumLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDateTime } from '../../lib/format';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { PageHeader } from '../../components/ui/PageHeader';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { SupplierApplication } from '../../types/supplier';

/** Applications from the public "Become a supplier" page. */
export default function SupplierApplicationsPage() {
  const table = useTableState('createdAt', 'desc');
  const [status, setStatus] = useState('PENDING');
  const statuses = useEnumLookup('application-statuses');

  const applications = useQuery({
    queryKey: ['supplier-applications', table.params, status],
    queryFn: () => supplierApplicationsApi.list(table.params, status || undefined),
    placeholderData: keepPreviousData,
  });

  const columns: Column<SupplierApplication>[] = [
    {
      key: 'company',
      header: 'Company',
      sortKey: 'companyName',
      render: (a) => (
        <Link to={String(a.id)} className="block">
          <span className="font-medium text-ink hover:text-primary">{a.companyName}</span>
          <span className="block text-xs text-muted">{a.registrationNo}</span>
        </Link>
      ),
    },
    { key: 'contact', header: 'Contact', render: (a) => (
      <div>
        <p>{a.contactPerson}</p>
        <p className="text-xs text-muted">{a.email}</p>
      </div>
    ) },
    { key: 'categories', header: 'Supplies', render: (a) => <span className="text-muted">{a.materialCategories.join(', ')}</span> },
    { key: 'status', header: 'Status', render: (a) => <StatusBadge status={a.status} /> },
    { key: 'received', header: 'Received', sortKey: 'createdAt', className: 'whitespace-nowrap', render: (a) => formatDateTime(a.createdAt) },
  ];

  return (
    <>
      <PageHeader title="Supplier applications" description="Approve a company to create its supplier account." />
      <DataTable
        columns={columns}
        page={applications.data}
        rowKey={(a) => a.id}
        loading={applications.isFetching}
        error={applications.isError ? toApiError(applications.error).message : null}
        emptyMessage={status === 'PENDING' ? 'No applications are waiting for review.' : 'No applications match these filters.'}
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Company, email or registration" />
            <div className="sm:ml-auto">
              <Select aria-label="Filter by status" options={statuses.data ?? []} placeholder="All statuses" value={status} onChange={(e) => { setStatus(e.target.value); table.resetPage(); }} />
            </div>
          </>
        }
      />
    </>
  );
}
