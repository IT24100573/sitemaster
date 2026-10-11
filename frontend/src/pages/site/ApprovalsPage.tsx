import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ClipboardCheck, Package, Receipt } from 'lucide-react';
import { dailyReportsApi } from '../../api/dailyReportsApi';
import { expensesApi } from '../../api/financeApi';
import { materialRequestsApi } from '../../api/materialRequestsApi';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { toApiError } from '../../lib/apiError';
import { formatDate, formatMoney } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { Card } from '../../components/ui/Card';
import { PageHeader } from '../../components/ui/PageHeader';
import { Spinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';

const QUEUE = { page: 0, size: 50, sort: 'createdAt', direction: 'asc' as const };

/** PM's queue: everything on their projects waiting for a decision, oldest first. */
export default function ApprovalsPage() {
  const currency = useCurrency();
  const requests = useQuery({
    queryKey: ['material-requests', 'queue'],
    queryFn: () => materialRequestsApi.list(QUEUE, { status: 'PENDING' }),
  });
  const reports = useQuery({
    queryKey: ['daily-reports', 'queue'],
    queryFn: () => dailyReportsApi.list(QUEUE, { status: 'PENDING' }),
  });
  const expenses = useQuery({
    queryKey: ['expenses', 'queue'],
    queryFn: () => expensesApi.list(QUEUE, { status: 'PENDING' }),
  });
  const pendingExpenses = (expenses.data?.content ?? []).filter((e) => e.canReview);
  const pendingRequests = (requests.data?.content ?? []).filter((r) => r.canReview);
  const pendingReports = (reports.data?.content ?? []).filter((r) => r.canReview);

  return (
    <>
      <PageHeader title="Approvals" description="Items from your projects waiting for your decision, oldest first." />
      <div className="grid gap-6 lg:grid-cols-2">
        <Card title={`Material requests (${pendingRequests.length})`}>
          {requests.isLoading ? <Spinner /> : requests.isError ? <Alert tone="error">{toApiError(requests.error).message}</Alert> : pendingRequests.length === 0 ? (
            <p className="text-sm text-muted">Nothing to approve.</p>
          ) : (
            <ul className="divide-y divide-line">
              {pendingRequests.map((r) => (
                <li key={r.id}>
                  <Link to={`../material-requests/${r.id}`} relative="path" className="flex items-center gap-3 py-3 hover:bg-surface">
                    <Package className="h-5 w-5 shrink-0 text-primary" aria-hidden />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-ink">{r.quantity} {r.unit} {r.materialName}</p>
                      <p className="truncate text-xs text-muted">{r.projectName}, {r.requestedByName}, needed {formatDate(r.requiredBy)}</p>
                    </div>
                    <span className="text-sm text-muted">{formatMoney(r.estimatedCost, currency)}</span>
                    <StatusBadge status={r.urgency} />
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card title={`Daily reports (${pendingReports.length})`}>
          {reports.isLoading ? <Spinner /> : reports.isError ? <Alert tone="error">{toApiError(reports.error).message}</Alert> : pendingReports.length === 0 ? (
            <p className="text-sm text-muted">Nothing to review.</p>
          ) : (
            <ul className="divide-y divide-line">
              {pendingReports.map((r) => (
                <li key={r.id}>
                  <Link to={`../daily-reports/${r.id}`} relative="path" className="flex items-center gap-3 py-3 hover:bg-surface">
                    <ClipboardCheck className="h-5 w-5 shrink-0 text-primary" aria-hidden />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-ink">{r.projectName}, {formatDate(r.reportDate)}</p>
                      <p className="truncate text-xs text-muted">{r.submittedByName}, {r.workerCount} workers</p>
                    </div>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
        <Card title={`Expenses (${pendingExpenses.length})`}>
          {expenses.isLoading ? <Spinner /> : expenses.isError ? <Alert tone="error">{toApiError(expenses.error).message}</Alert> : pendingExpenses.length === 0 ? (
            <p className="text-sm text-muted">Nothing to approve.</p>
          ) : (
            <ul className="divide-y divide-line">
              {pendingExpenses.map((e) => (
                <li key={e.id}>
                  <Link to={`../expenses/${e.id}`} relative="path" className="flex items-center gap-3 py-3 hover:bg-surface">
                    <Receipt className="h-5 w-5 shrink-0 text-primary" aria-hidden />
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-ink">{e.payee}</p>
                      <p className="truncate text-xs text-muted">{e.projectName}, {e.requestedByName}, {formatDate(e.expenseDate)}</p>
                    </div>
                    <span className="text-sm text-muted">{formatMoney(e.amount, currency)}</span>
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  );
}
