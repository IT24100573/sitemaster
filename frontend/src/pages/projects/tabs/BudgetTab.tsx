import { useQuery } from '@tanstack/react-query';
import { financeApi } from '../../../api/financeApi';
import { projectsApi } from '../../../api/projectsApi';
import { useCurrentUser } from '../../../context/AuthContext';
import { useCurrency } from '../../../hooks/useCompanyProfile';
import { toApiError } from '../../../lib/apiError';
import { formatMoney, humanize } from '../../../lib/format';
import { Alert } from '../../../components/ui/Alert';
import { Card } from '../../../components/ui/Card';
import { ProgressBar } from '../../../components/ui/ProgressBar';
import { Spinner } from '../../../components/ui/Spinner';
import { projectKeys } from '../queryKeys';
import type { ProjectDetail } from '../../../types/project';

const FINANCE_ROLES = ['ADMIN', 'ACCOUNTANT', 'PROJECT_MANAGER'];

/**
 * Budget split across phases for everyone on the project; spending, commitments
 * and billing for the Admin, the Accountant and the project's manager.
 */
export function BudgetTab({ project }: { project: ProjectDetail }) {
  const user = useCurrentUser();
  const currency = useCurrency();
  const showFinance = FINANCE_ROLES.includes(user.role);
  const phases = useQuery({ queryKey: projectKeys.phases(project.id), queryFn: () => projectsApi.phases(project.id) });
  const finance = useQuery({ queryKey: ['finance', 'project', project.id], queryFn: () => financeApi.project(project.id), enabled: showFinance, retry: false });
  const budget = Number(project.budget);

  const figures = finance.data
    ? [
        { label: 'Budget', value: finance.data.budget, highlighted: true },
        { label: 'Spent', value: finance.data.totalSpent },
        { label: 'Committed', value: finance.data.committed },
        { label: 'Remaining', value: finance.data.remaining, danger: Number(finance.data.remaining) < 0 },
      ]
    : [
        { label: 'Project budget', value: budget, highlighted: true },
        { label: 'Allocated to phases', value: Number(project.allocatedBudget) },
        { label: 'Not yet allocated', value: budget - Number(project.allocatedBudget) },
      ];

  return (
    <div className="space-y-6">
      {showFinance && finance.isLoading && <Spinner />}
      {showFinance && finance.isError && <Alert tone="error">{toApiError(finance.error).message}</Alert>}
      <div className={`grid gap-4 ${figures.length === 4 ? 'md:grid-cols-4' : 'md:grid-cols-3'}`}>
        {figures.map((item) => (
          <Card key={item.label} highlighted={item.highlighted}>
            <p className="text-sm text-muted">{item.label}</p>
            <p className={`mt-1 text-xl font-semibold ${'danger' in item && item.danger ? 'text-red-600' : 'text-ink'}`}>{formatMoney(item.value, currency)}</p>
          </Card>
        ))}
      </div>

      {finance.data && (
        <div className="grid gap-6 lg:grid-cols-2">
          <Card title="Spending by category" description={`${finance.data.usedPercent}% of the budget is spent or committed.`}>
            <ul className="space-y-3">
              {finance.data.byCategory.map((c) => {
                const share = Number(finance.data.totalSpent) > 0 ? (Number(c.amount) / Number(finance.data.totalSpent)) * 100 : 0;
                return (
                  <li key={c.category}>
                    <div className="mb-1 flex justify-between text-sm"><span>{humanize(c.category)}</span><span className="text-muted">{formatMoney(c.amount, currency)}</span></div>
                    <ProgressBar value={share} label={`${humanize(c.category)} share of spending`} />
                  </li>
                );
              })}
            </ul>
          </Card>
          <Card title="Billing">
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between"><dt className="text-muted">Invoiced to client</dt><dd>{formatMoney(finance.data.invoiced, currency)}</dd></div>
              <div className="flex justify-between"><dt className="text-muted">Received</dt><dd>{formatMoney(finance.data.received, currency)}</dd></div>
              <div className="flex justify-between font-medium"><dt>Still owed by client</dt><dd>{formatMoney(finance.data.receivable, currency)}</dd></div>
              {finance.data.pettyCashBalance !== null && (
                <div className="flex justify-between border-t border-line pt-2"><dt className="text-muted">Petty cash on site</dt><dd>{formatMoney(finance.data.pettyCashBalance, currency)}</dd></div>
              )}
            </dl>
          </Card>
        </div>
      )}

      <Card title="Budget by phase" description={finance.data ? 'Committed includes approved material requests and expenses.' : undefined}>
        {phases.isLoading ? <Spinner /> : phases.data && phases.data.length > 0 ? (
          <ul className="space-y-4">
            {phases.data.map((phase) => {
              const position = finance.data?.phases.find((p) => p.phaseId === phase.id);
              const phaseBudget = Number(phase.budget);
              const value = position
                ? (phaseBudget > 0 ? (Number(position.committed) / phaseBudget) * 100 : 0)
                : (budget > 0 ? (phaseBudget / budget) * 100 : 0);
              return (
                <li key={phase.id}>
                  <div className="mb-1 flex justify-between gap-4 text-sm">
                    <span className="font-medium text-ink">{phase.name}</span>
                    <span className={position && Number(position.remaining) < 0 ? 'text-red-600' : 'text-muted'}>
                      {position ? `${formatMoney(position.committed, currency)} of ${formatMoney(phase.budget, currency)}` : formatMoney(phase.budget, currency)}
                    </span>
                  </div>
                  <ProgressBar value={Math.min(value, 100)} label={position ? `${phase.name} budget used` : `${phase.name} share of budget`} />
                </li>
              );
            })}
          </ul>
        ) : (
          <p className="text-sm text-muted">No phases yet. Add phases on the Timeline tab to split the budget.</p>
        )}
      </Card>
    </div>
  );
}
