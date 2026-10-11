import { useState } from 'react';
import { Link } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { employeesApi } from '../../api/employeesApi';
import { useAuth } from '../../context/AuthContext';
import { useLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDate, humanize } from '../../lib/format';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { PageHeader } from '../../components/ui/PageHeader';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { EmployeeFilters, EmployeeListItem } from '../../types/employee';

const STATUS_OPTIONS = [
  { value: 'true', label: 'Active' },
  { value: 'false', label: 'Inactive' },
];

/** Employee register (HR manages, Admin views). */
export default function EmployeesPage() {
  const { hasPermission } = useAuth();
  const table = useTableState('employeeNo');
  const [filters, setFilters] = useState<EmployeeFilters>({});
  const types = useLookup('employmentTypes');
  const trades = useLookup('trades');

  const employees = useQuery({
    queryKey: ['employees', table.params, filters],
    queryFn: () => employeesApi.list(table.params, filters),
    placeholderData: keepPreviousData,
  });

  const setFilter = (key: keyof EmployeeFilters, value: string) => {
    setFilters((current) => ({ ...current, [key]: value || undefined }));
    table.resetPage();
  };

  const columns: Column<EmployeeListItem>[] = [
    {
      key: 'no',
      header: 'No.',
      sortKey: 'employeeNo',
      className: 'whitespace-nowrap',
      render: (e) => (
        <Link to={String(e.id)} className="font-medium text-primary hover:text-primary-dark">
          {e.employeeNo}
        </Link>
      ),
    },
    {
      key: 'name',
      header: 'Name',
      sortKey: 'fullName',
      render: (e) => (
        <div>
          <p className="font-medium text-ink">{e.fullName}</p>
          <p className="text-xs text-muted">{e.roleDisplayName}</p>
        </div>
      ),
    },
    { key: 'type', header: 'Employment', sortKey: 'employmentType', render: (e) => humanize(e.employmentType) },
    { key: 'trade', header: 'Trade', render: (e) => e.trade ?? '-' },
    { key: 'joined', header: 'Joined', sortKey: 'joinDate', render: (e) => formatDate(e.joinDate) },
    { key: 'status', header: 'Status', render: (e) => <StatusBadge status={e.active ? 'ACTIVE' : 'INACTIVE'} /> },
  ];

  return (
    <>
      <PageHeader
        title="Employees"
        description="Everyone on the payroll: staff, contract and daily-wage workers."
        actions={
          hasPermission('EMPLOYEE:CREATE') && (
            <Link
              to="new"
              className="inline-flex h-11 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-white hover:bg-primary-dark"
            >
              <Plus className="h-4 w-4" aria-hidden />
              Register employee
            </Link>
          )
        }
      />
      <DataTable
        columns={columns}
        page={employees.data}
        rowKey={(e) => e.id}
        loading={employees.isFetching}
        error={employees.isError ? toApiError(employees.error).message : null}
        emptyMessage="No employees match these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Name, number, NIC or email" />
            <div className="flex flex-wrap gap-2 sm:ml-auto">
              <Select
                aria-label="Filter by employment type"
                options={types.data ?? []}
                placeholder="All types"
                value={filters.employmentType ?? ''}
                onChange={(e) => setFilter('employmentType', e.target.value)}
              />
              <Select
                aria-label="Filter by trade"
                options={trades.data ?? []}
                placeholder="All trades"
                value={filters.trade ?? ''}
                onChange={(e) => setFilter('trade', e.target.value)}
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
    </>
  );
}
