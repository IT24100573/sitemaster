import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { expensesApi } from '../../api/financeApi';
import { useAuth, useCurrentUser } from '../../context/AuthContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useEnumLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { toApiError } from '../../lib/apiError';
import { formatDate, formatMoney, humanize } from '../../lib/format';
import { Button } from '../../components/ui/Button';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { ExpenseForm } from './ExpenseForm';
import type { Expense } from '../../types/finance';

const STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'PAID'];

/** Expense requests for the user's projects; the Accountant sees approved ones first. */
export default function ExpensesPage() {
  const user = useCurrentUser();
  const { hasPermission } = useAuth();
  const navigate = useNavigate();
  const currency = useCurrency();
  const table = useTableState('createdAt', 'desc');
  const [projectId, setProjectId] = useState('');
  const [status, setStatus] = useState(user.role === 'ACCOUNTANT' ? 'APPROVED' : '');
  const [category, setCategory] = useState('');
  const [creating, setCreating] = useState(false);
  const categories = useEnumLookup('expense-categories');
  const requestStatuses = useEnumLookup('request-statuses');
  const canCreate = hasPermission('EXPENSE:CREATE') && (user.role === 'SITE_ENGINEER' || user.role === 'ACCOUNTANT');

  const expenses = useQuery({
    queryKey: ['expenses', table.params, projectId, status, category],
    queryFn: () => expensesApi.list(table.params, { projectId: projectId || undefined, status: status || undefined, category: category || undefined }),
    placeholderData: keepPreviousData,
  });

  const columns: Column<Expense>[] = [
    { key: 'date', header: 'Date', sortKey: 'expenseDate', className: 'whitespace-nowrap', render: (e) => formatDate(e.expenseDate) },
    { key: 'payee', header: 'Paid to', render: (e) => (
      <Link to={String(e.id)} className="block">
        <span className="font-medium text-ink hover:text-primary">{e.payee}</span>
        <span className="block line-clamp-1 text-xs text-muted">{e.description}</span>
      </Link>
    ) },
    { key: 'project', header: 'Project', render: (e) => `${e.projectName}, ${e.phaseName}` },
    { key: 'category', header: 'Category', render: (e) => humanize(e.category) },
    { key: 'amount', header: 'Amount', sortKey: 'amount', className: 'whitespace-nowrap text-right', render: (e) => formatMoney(e.amount, currency) },
    { key: 'status', header: 'Status', sortKey: 'status', render: (e) => <StatusBadge status={e.status} /> },
  ];

  return (
    <>
      <PageHeader title="Expenses" description="Site costs: raised on site, approved by the project manager, paid by the accountant."
        actions={canCreate && <Button onClick={() => setCreating(true)}><Plus className="h-4 w-4" aria-hidden />New expense</Button>} />
      <DataTable
        columns={columns}
        page={expenses.data}
        rowKey={(e) => e.id}
        loading={expenses.isFetching}
        error={expenses.isError ? toApiError(expenses.error).message : null}
        emptyMessage="No expenses match these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Payee or description" />
            <div className="flex flex-wrap gap-2 sm:ml-auto">
              <ProjectSelect aria-label="Filter by project" value={projectId} onChange={(e) => { setProjectId(e.target.value); table.resetPage(); }} />
              <Select aria-label="Filter by category" options={categories.data ?? []} placeholder="Any category" value={category} onChange={(e) => { setCategory(e.target.value); table.resetPage(); }} />
              <Select aria-label="Filter by status" options={(requestStatuses.data ?? []).filter((s) => STATUSES.includes(s.value))} placeholder="Any status" value={status} onChange={(e) => { setStatus(e.target.value); table.resetPage(); }} />
            </div>
          </>
        }
      />
      <Modal open={creating} size="lg" title="New expense" onClose={() => setCreating(false)}>
        {creating && <ExpenseForm onDone={(saved) => { setCreating(false); if (saved) navigate(String(saved.id)); }} />}
      </Modal>
    </>
  );
}
