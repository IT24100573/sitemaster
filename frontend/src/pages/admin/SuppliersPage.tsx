import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Power } from 'lucide-react';
import { suppliersApi } from '../../api/suppliersApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { PageHeader } from '../../components/ui/PageHeader';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { SupplierRecord } from '../../types/supplier';

const STATUS_OPTIONS = [
  { value: 'true', label: 'Active' },
  { value: 'false', label: 'Inactive' },
];

/** Approved suppliers. Deactivating one also blocks its sign-in. */
export default function SuppliersPage() {
  const { hasPermission } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const table = useTableState('name');
  const [active, setActive] = useState('');
  const [category, setCategory] = useState('');
  const [toggling, setToggling] = useState<SupplierRecord | null>(null);
  const categories = useLookup('materialCategories');

  const suppliers = useQuery({
    queryKey: ['suppliers', table.params, active, category],
    queryFn: () => suppliersApi.list(table.params, active || undefined, category || undefined),
    placeholderData: keepPreviousData,
  });

  const toggle = useMutation({
    mutationFn: (s: SupplierRecord) => suppliersApi.setActive(s.id, !s.active),
    onSuccess: async (s) => {
      setToggling(null);
      await queryClient.invalidateQueries({ queryKey: ['suppliers'] });
      toast.success(`${s.name} is now ${s.active ? 'active' : 'inactive'}.`);
    },
    onError: (error) => {
      setToggling(null);
      toast.error(toApiError(error).message);
    },
  });

  const columns: Column<SupplierRecord>[] = [
    {
      key: 'name',
      header: 'Supplier',
      sortKey: 'name',
      render: (s) => (
        <div>
          <p className="font-medium text-ink">{s.name}</p>
          <p className="text-xs text-muted">{s.registrationNo ?? '-'}</p>
        </div>
      ),
    },
    { key: 'contact', header: 'Contact', render: (s) => (
      <div>
        <p>{s.contactPerson}</p>
        <p className="text-xs text-muted">{s.email}, {s.phone}</p>
      </div>
    ) },
    { key: 'categories', header: 'Supplies', render: (s) => <span className="text-muted">{s.materialCategories.join(', ')}</span> },
    { key: 'status', header: 'Status', render: (s) => <StatusBadge status={s.active ? 'ACTIVE' : 'INACTIVE'} /> },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (s) =>
        hasPermission('SUPPLIER:EDIT') && (
          <button type="button" onClick={() => setToggling(s)} className={`rounded-md p-2 hover:bg-surface ${s.active ? 'text-red-600' : 'text-green-700'}`} aria-label={s.active ? `Deactivate ${s.name}` : `Activate ${s.name}`}>
            <Power className="h-4 w-4" aria-hidden />
          </button>
        ),
    },
  ];

  return (
    <>
      <PageHeader title="Suppliers" description="Companies approved to receive purchase orders." />
      <DataTable
        columns={columns}
        page={suppliers.data}
        rowKey={(s) => s.id}
        loading={suppliers.isFetching}
        error={suppliers.isError ? toApiError(suppliers.error).message : null}
        emptyMessage="No suppliers yet. Approve an application to add one."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Name or email" />
            <div className="flex gap-2 sm:ml-auto">
              <Select aria-label="Filter by material" options={categories.data ?? []} placeholder="Any material" value={category} onChange={(e) => { setCategory(e.target.value); table.resetPage(); }} />
              <Select aria-label="Filter by status" options={STATUS_OPTIONS} placeholder="Any status" value={active} onChange={(e) => { setActive(e.target.value); table.resetPage(); }} />
            </div>
          </>
        }
      />
      <ConfirmDialog
        open={toggling !== null}
        title={toggling?.active ? `Deactivate ${toggling.name}?` : `Activate ${toggling?.name ?? ''}?`}
        message={toggling?.active ? 'The supplier is signed out and cannot sign in or receive new purchase orders.' : 'The supplier can sign in and receive purchase orders again.'}
        confirmLabel={toggling?.active ? 'Deactivate' : 'Activate'}
        tone={toggling?.active ? 'danger' : 'primary'}
        loading={toggle.isPending}
        onConfirm={() => toggling && toggle.mutate(toggling)}
        onCancel={() => setToggling(null)}
      />
    </>
  );
}
