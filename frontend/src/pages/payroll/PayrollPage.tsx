import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, Send } from 'lucide-react';
import { payrollApi } from '../../api/workforceApi';
import { useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { toApiError } from '../../lib/apiError';
import { formatDateTime, formatMoney, formatMonth, humanize } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { PageHeader } from '../../components/ui/PageHeader';
import { Spinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { SummaryTable } from './SummaryTable';

function previousMonth(): string {
  const d = new Date();
  d.setDate(1);
  d.setMonth(d.getMonth() - 1);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

/** HR prepares and sends the monthly summary; everyone with access sees the runs. */
export default function PayrollPage() {
  const user = useCurrentUser();
  const isHr = user.role === 'HR_MANAGER';
  const currency = useCurrency();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [value, setValue] = useState(previousMonth());
  const [confirm, setConfirm] = useState(false);
  const [year, month] = value.split('-').map(Number);
  const now = new Date();
  const maxMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;

  const runs = useQuery({ queryKey: ['payroll', 'runs'], queryFn: payrollApi.runs });
  const preview = useQuery({
    queryKey: ['payroll', 'preview', value],
    queryFn: () => payrollApi.preview(year, month),
    enabled: isHr && !!year && !!month,
  });
  const send = useMutation({
    mutationFn: () => payrollApi.sendSummary(year, month),
    onSuccess: async () => {
      setConfirm(false);
      await queryClient.invalidateQueries({ queryKey: ['payroll'] });
      await queryClient.invalidateQueries({ queryKey: ['attendance'] });
      toast.success(`${formatMonth(year, month)} summary sent to the accountants. That month's attendance is now locked.`);
    },
    onError: (error) => {
      setConfirm(false);
      toast.error(toApiError(error).message);
    },
  });

  const p = preview.data;
  const waiting = p ? p.unverifiedAttendance + p.pendingOvertime + p.pendingLeave : 0;

  return (
    <>
      <PageHeader title="Payroll" description={isHr ? 'Check the month\u2019s attendance and send the summary to the accountants.' : 'Monthly payroll runs. Open a run to calculate and pay.'} />

      {isHr && (
        <Card title="Prepare a summary" className="mb-6" actions={
          <input type="month" aria-label="Payroll month" value={value} max={maxMonth} onChange={(e) => setValue(e.target.value)} className="form-input w-auto" />
        }>
          {preview.isLoading ? <Spinner /> : preview.isError ? <Alert tone="error">{toApiError(preview.error).message}</Alert> : p && (
            p.alreadySent ? (
              <p className="text-sm text-muted">The {formatMonth(p.year, p.month)} summary has already been sent.</p>
            ) : (
              <div className="space-y-4">
                <p className="text-sm text-muted">{p.workingDays} working days, {p.rows.length} employee(s). Only verified attendance and approved overtime and leave are counted.</p>
                {waiting > 0 && (
                  <div className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">
                    <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
                    <span>
                      Still waiting: {p.unverifiedAttendance} unverified attendance record(s), {p.pendingOvertime} overtime and {p.pendingLeave} leave request(s).
                      These will not be paid if you send now, and the month will be locked.
                    </span>
                  </div>
                )}
                <SummaryTable rows={p.rows} />
                <Button onClick={() => setConfirm(true)} disabled={p.rows.length === 0}>
                  <Send className="h-4 w-4" aria-hidden />
                  Send summary to accountants
                </Button>
              </div>
            )
          )}
        </Card>
      )}

      <Card title="Payroll runs">
        {runs.isLoading ? <Spinner /> : runs.isError ? <Alert tone="error">{toApiError(runs.error).message}</Alert> : (runs.data ?? []).length === 0 ? (
          <p className="text-sm text-muted">No payroll has been run yet.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[640px] text-sm">
              <thead className="text-left text-muted">
                <tr className="border-b border-line">
                  <th scope="col" className="py-2 pr-3 font-medium">Month</th>
                  <th scope="col" className="py-2 pr-3 font-medium">Status</th>
                  <th scope="col" className="py-2 pr-3 font-medium">Employees</th>
                  <th scope="col" className="py-2 pr-3 font-medium">Summary sent</th>
                  <th scope="col" className="py-2 text-right font-medium">Net total</th>
                </tr>
              </thead>
              <tbody>
                {runs.data?.map((r) => (
                  <tr key={r.id} className="border-b border-line last:border-0">
                    <td className="py-2.5 pr-3"><Link to={String(r.id)} className="font-medium text-primary hover:text-primary-dark">{formatMonth(r.year, r.month)}</Link></td>
                    <td className="py-2.5 pr-3"><StatusBadge status={r.status} label={humanize(r.status)} /></td>
                    <td className="py-2.5 pr-3">{r.employees}</td>
                    <td className="py-2.5 pr-3 text-muted">{r.summarySentByName}, {formatDateTime(r.summarySentAt)}</td>
                    <td className="py-2.5 text-right">{r.status === 'SUMMARY_SENT' ? '-' : formatMoney(r.totalNet, currency)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>

      <ConfirmDialog
        open={confirm}
        title={`Send the ${formatMonth(year, month)} summary?`}
        message="The accountants will be notified and this month's attendance, overtime and leave can no longer be changed."
        confirmLabel="Send summary"
        loading={send.isPending}
        onConfirm={() => send.mutate()}
        onCancel={() => setConfirm(false)}
      />
    </>
  );
}
