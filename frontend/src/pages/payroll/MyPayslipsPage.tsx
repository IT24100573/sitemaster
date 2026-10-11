import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { ChevronDown, Download } from 'lucide-react';
import { payrollApi } from '../../api/workforceApi';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { toApiError } from '../../lib/apiError';
import { formatDateTime, formatMoney, formatMonth } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { PageHeader } from '../../components/ui/PageHeader';
import { Spinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { PayslipLines } from './PayslipLines';

/** Employee's own payslips with every calculation step and a PDF download. */
export default function MyPayslipsPage() {
  const currency = useCurrency();
  const [open, setOpen] = useState<number | null>(null);
  const payslips = useQuery({ queryKey: ['payslips', 'mine'], queryFn: payrollApi.myPayslips });

  return (
    <>
      <PageHeader title="My payslips" description="Payslips appear once the accountant has calculated the month's payroll." />
      {payslips.isLoading ? <Spinner /> : payslips.isError ? (
        <Alert tone="error">{toApiError(payslips.error).message}</Alert>
      ) : (payslips.data ?? []).length === 0 ? (
        <p className="card p-8 text-center text-sm text-muted">No payslips yet.</p>
      ) : (
        <ul className="space-y-3">
          {payslips.data?.map((p) => (
            <li key={p.id} className="card">
              <div className="flex flex-wrap items-center gap-3 p-4">
                <button type="button" onClick={() => setOpen(open === p.id ? null : p.id)} className="flex flex-1 items-center gap-3 text-left" aria-expanded={open === p.id}>
                  <ChevronDown className={`h-4 w-4 text-muted transition-transform ${open === p.id ? 'rotate-180' : ''}`} aria-hidden />
                  <span className="font-medium text-ink">{formatMonth(p.year, p.month)}</span>
                  {p.paid ? <StatusBadge status="PAID" label={`Paid ${formatDateTime(p.paidAt)}`} /> : <StatusBadge status="CALCULATED" label="Not yet paid" />}
                </button>
                <span className="text-lg font-semibold text-ink">{formatMoney(p.netPay, currency)}</span>
                <a href={payrollApi.pdfUrl(p.id)} className="inline-flex h-9 items-center gap-1 rounded-lg border border-line px-3 text-sm font-medium text-ink hover:bg-surface">
                  <Download className="h-4 w-4" aria-hidden />
                  PDF
                </a>
              </div>
              {open === p.id && <div className="border-t border-line p-4"><PayslipLines payslip={p} /></div>}
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
