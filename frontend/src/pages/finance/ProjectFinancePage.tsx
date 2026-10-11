import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { financeApi } from '../../api/financeApi';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { toApiError } from '../../lib/apiError';
import { formatMoney } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { Spinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';

/** Budget against spending and billing for every project the user can see. */
export default function ProjectFinancePage() {
  const currency = useCurrency();
  const overview = useQuery({ queryKey: ['finance', 'overview'], queryFn: financeApi.overview });

  return (
    <>
      <PageHeader title="Project finance" description="Spent is money paid out; committed is approved but not yet paid." />
      {overview.isLoading ? <Spinner /> : overview.isError ? <Alert tone="error">{toApiError(overview.error).message}</Alert> : (
        <div className="card overflow-x-auto p-0">
          <table className="w-full min-w-[900px] text-sm">
            <thead className="bg-surface text-left text-muted">
              <tr>
                <th scope="col" className="px-4 py-3 font-medium">Project</th>
                <th scope="col" className="px-4 py-3 text-right font-medium">Budget</th>
                <th scope="col" className="px-4 py-3 text-right font-medium">Spent</th>
                <th scope="col" className="px-4 py-3 text-right font-medium">Committed</th>
                <th scope="col" className="px-4 py-3 text-right font-medium">Remaining</th>
                <th scope="col" className="w-40 px-4 py-3 font-medium">Used</th>
                <th scope="col" className="px-4 py-3 text-right font-medium">Invoiced</th>
                <th scope="col" className="px-4 py-3 text-right font-medium">Owed by client</th>
              </tr>
            </thead>
            <tbody>
              {overview.data?.map((p) => (
                <tr key={p.projectId} className="border-t border-line">
                  <td className="px-4 py-3">
                    <Link to={`../projects/${p.projectId}`} relative="path" className="font-medium text-ink hover:text-primary">{p.projectName}</Link>
                    <div className="mt-1"><StatusBadge status={p.status} /></div>
                  </td>
                  <td className="px-4 py-3 text-right">{formatMoney(p.budget, currency)}</td>
                  <td className="px-4 py-3 text-right">{formatMoney(p.totalSpent, currency)}</td>
                  <td className="px-4 py-3 text-right">{formatMoney(p.committed, currency)}</td>
                  <td className={`px-4 py-3 text-right ${Number(p.remaining) < 0 ? 'font-semibold text-red-600' : ''}`}>{formatMoney(p.remaining, currency)}</td>
                  <td className="px-4 py-3"><ProgressBar value={Math.min(p.usedPercent, 100)} label={`${p.projectName} budget used`} /><span className="text-xs text-muted">{p.usedPercent}%</span></td>
                  <td className="px-4 py-3 text-right">{formatMoney(p.invoiced, currency)}</td>
                  <td className="px-4 py-3 text-right">{formatMoney(p.receivable, currency)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}
