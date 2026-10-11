import { useState } from 'react';
import { Link } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { purchasingApi, supplierPortalApi } from '../../api/purchasingApi';
import { useCurrentUser } from '../../context/AuthContext';
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
import type { PurchaseOrder } from '../../types/purchase';

/** Purchase orders: suppliers see their own; staff see orders for their projects. */
export default function PurchaseOrdersPage() {
  const user = useCurrentUser();
  const isSupplier = user.role === 'SUPPLIER';
  const currency = useCurrency();
  const table = useTableState('createdAt', 'desc');
  const [status, setStatus] = useState('');
  const [projectId, setProjectId] = useState('');
  const statuses = useEnumLookup('purchase-order-statuses');

  const orders = useQuery({
    queryKey: ['purchase-orders', table.params, status, projectId, isSupplier],
    queryFn: () =>
      isSupplier
        ? supplierPortalApi.orders(table.params, status || undefined)
        : purchasingApi.orders(table.params, { status: status || undefined, projectId: projectId || undefined }),
    placeholderData: keepPreviousData,
  });

  const columns: Column<PurchaseOrder>[] = [
    {
      key: 'number',
      header: 'Order',
      sortKey: 'poNumber',
      render: (o) => (
        <Link to={String(o.id)} className="block">
          <span className="font-medium text-ink hover:text-primary">{o.poNumber}</span>
          <span className="block text-xs text-muted">{o.items.map((i) => `${i.quantity} ${i.unit} ${i.materialName}`).join(', ')}</span>
        </Link>
      ),
    },
    isSupplier
      ? { key: 'site', header: 'Deliver to', render: (o) => `${o.projectName}, ${o.deliveryLocation}` }
      : { key: 'supplier', header: 'Supplier', render: (o) => o.supplierName },
    ...(isSupplier ? [] : [{ key: 'project', header: 'Project', render: (o: PurchaseOrder) => o.projectName }]),
    { key: 'date', header: 'Ordered', sortKey: 'orderDate', className: 'whitespace-nowrap', render: (o) => formatDate(o.orderDate) },
    { key: 'total', header: 'Total', sortKey: 'totalAmount', className: 'whitespace-nowrap text-right', render: (o) => formatMoney(o.totalAmount, currency) },
    { key: 'status', header: 'Status', sortKey: 'status', render: (o) => <StatusBadge status={o.status} /> },
  ];

  return (
    <>
      <PageHeader
        title="Purchase orders"
        description={isSupplier ? 'Orders sent to your company. Accept or decline new orders, then dispatch and invoice.' : 'Orders raised for approved material requests.'}
      />
      <DataTable
        columns={columns}
        page={orders.data}
        rowKey={(o) => o.id}
        loading={orders.isFetching}
        error={orders.isError ? toApiError(orders.error).message : null}
        emptyMessage="No purchase orders match these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="PO number" />
            <div className="flex flex-wrap gap-2 sm:ml-auto">
              {!isSupplier && <ProjectSelect aria-label="Filter by project" value={projectId} onChange={(e) => { setProjectId(e.target.value); table.resetPage(); }} />}
              <Select aria-label="Filter by status" options={statuses.data ?? []} placeholder="Any status" value={status} onChange={(e) => { setStatus(e.target.value); table.resetPage(); }} />
            </div>
          </>
        }
      />
    </>
  );
}
