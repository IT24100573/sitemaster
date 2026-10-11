import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Calculator, ChevronDown, Download } from 'lucide-react';
import { payrollApi } from '../../api/workforceApi';
import { useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useEnumLookup } from '../../hooks/useLookup';
import { toApiError } from '../../lib/apiError';
import { formatDateTime, formatMoney, formatMonth, humanize } from '../../lib/format';
import { CLIENT_MESSAGES } from '../../validation/messages';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { DetailList } from '../../components/ui/DetailList';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { Select } from '../../components/ui/Select';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { PayslipLines } from './PayslipLines';
import { SummaryTable } from './SummaryTable';
import type { Payslip } from '../../types/workforce';

/** One payroll run: HR's summary, calculation (Accountant) and salary payments. */
export default function PayrollRunPage() {
  const { id } = useParams();
  const runId = Number(id);
  const user = useCurrentUser();
  const isAccountant = user.role === 'ACCOUNTANT';
  const currency = useCurrency();
  const toast = useToast();
  const queryClient = useQueryClient();
  const methods = useEnumLookup('payment-methods');
  const key = ['payroll', 'run', runId];
  const [open, setOpen] = useState<number | null>(null);
  const [paying, setPaying] = useState<{ payslip: Payslip | null } | null>(null);
  const [method, setMethod] = useState('BANK_TRANSFER');
  const [reference, setReference] = useState('');
  const [error, setError] = useState<string | undefined>();

  const detail = useQuery({ queryKey: key, queryFn: () => payrollApi.run(runId) });

  const calculate = useMutation({
    mutationFn: () => payrollApi.calculate(runId),
    onSuccess: async (d) => {
      queryClient.setQueryData(key, d);
      await queryClient.invalidateQueries({ queryKey: ['payroll', 'runs'] });
      toast.success(`Payroll calculated: net ${formatMoney(d.run.totalNet, currency)}.`);
    },
    onError: (err) => toast.error(toApiError(err).message),
  });

  const pay = useMutation({
    mutationFn: async () => {
      const ref = method === 'CASH' ? null : reference.trim();
      if (paying?.payslip) {
        await payrollApi.paySlip(paying.payslip.id, method, ref);
      } else {
        await payrollApi.payAll(runId, method, ref);
      }
    },
    onSuccess: async () => {
      setPaying(null);
      await queryClient.invalidateQueries({ queryKey: ['payroll'] });
      toast.success(paying?.payslip ? `${paying.payslip.employeeName} paid.` : 'Salaries paid. Employees have been notified.');
    },
    onError: (err) => {
      const apiError = toApiError(err);
      setError(apiError.fieldErrors.reference ?? apiError.message);
    },
  });

  if (detail.isLoading) {
    return <FullPageSpinner />;
  }
  if (detail.isError || !detail.data) {
    return <Alert tone="error">{toApiError(detail.error).message}</Alert>;
  }
  const { run, summaries, payslips } = detail.data;
  const canCalculate = isAccountant && run.status !== 'PAID';
  const canPay = isAccountant && run.status === 'CALCULATED';

  const startPaying = (payslip: Payslip | null) => {
    setPaying({ payslip });
    setMethod('BANK_TRANSFER');
    setReference('');
    setError(undefined);
  };
  const submitPayment = () => {
    if (method !== 'CASH' && !/^[A-Za-z0-9/-]{3,50}$/.test(reference.trim())) {
      setError(CLIENT_MESSAGES.reference);
      return;
    }
    pay.mutate();
  };

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        Back to payroll
      </Link>
      <PageHeader
        title={`${formatMonth(run.year, run.month)} payroll`}
        description={<span className="flex items-center gap-2"><StatusBadge status={run.status} label={humanize(run.status)} />{run.employees} employee(s), {run.workingDays} working days</span>}
        actions={
          <>
            {canCalculate && (
              <Button variant={run.status === 'CALCULATED' ? 'secondary' : 'primary'} loading={calculate.isPending} onClick={() => calculate.mutate()}>
                <Calculator className="h-4 w-4" aria-hidden />
                {run.status === 'CALCULATED' ? 'Recalculate' : 'Calculate'}
              </Button>
            )}
            {canPay && run.unpaidPayslips > 0 && <Button onClick={() => startPaying(null)}>Pay all ({run.unpaidPayslips})</Button>}
          </>
        }
      />

      <div className="mb-6 grid gap-6 lg:grid-cols-3">
        <Card title="Totals">
          <DetailList items={[
            { label: 'Gross', value: formatMoney(run.totalGross, currency) },
            { label: 'Deductions', value: formatMoney(run.totalDeductions, currency) },
            { label: 'Net', value: <span className="font-semibold">{formatMoney(run.totalNet, currency)}</span> },
          ]} />
        </Card>
        <Card title="History" className="lg:col-span-2">
          <DetailList items={[
            { label: 'Summary sent', value: `${run.summarySentByName}, ${formatDateTime(run.summarySentAt)}` },
            { label: 'Calculated', value: run.calculatedAt ? `${run.calculatedByName}, ${formatDateTime(run.calculatedAt)}` : 'Not yet' },
            { label: 'Paid', value: run.paidAt ? formatDateTime(run.paidAt) : run.status === 'CALCULATED' ? `${run.unpaidPayslips} payslip(s) still to pay` : 'Not yet' },
          ]} />
        </Card>
      </div>

      <Card title="Payslips" className="mb-6">
        {payslips.length === 0 ? (
          <p className="text-sm text-muted">{isAccountant ? 'Calculate the payroll to create payslips.' : 'The accountant has not calculated this payroll yet.'}</p>
        ) : (
          <ul className="divide-y divide-line">
            {payslips.map((p) => (
              <li key={p.id} className="py-2">
                <div className="flex flex-wrap items-center gap-3">
                  <button type="button" onClick={() => setOpen(open === p.id ? null : p.id)} className="flex flex-1 items-center gap-2 text-left" aria-expanded={open === p.id}>
                    <ChevronDown className={`h-4 w-4 text-muted transition-transform ${open === p.id ? 'rotate-180' : ''}`} aria-hidden />
                    <span className="font-medium text-ink">{p.employeeName}</span>
                    <span className="text-xs text-muted">{p.employeeNo}, {humanize(p.employmentType)}</span>
                  </button>
                  <span className="text-sm text-muted">Gross {formatMoney(p.grossPay, currency)}</span>
                  <span className="font-semibold text-ink">{formatMoney(p.netPay, currency)}</span>
                  {p.paid ? <StatusBadge status="PAID" /> : canPay && <Button size="sm" variant="secondary" onClick={() => startPaying(p)}>Pay</Button>}
                  <a href={payrollApi.pdfUrl(p.id)} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Download payslip of ${p.employeeName}`}>
                    <Download className="h-4 w-4" aria-hidden />
                  </a>
                </div>
                {open === p.id && <div className="mt-3 rounded-lg bg-surface p-3"><PayslipLines payslip={p} /></div>}
              </li>
            ))}
          </ul>
        )}
      </Card>

      <Card title="HR attendance summary">
        <SummaryTable rows={summaries} />
      </Card>

      <Modal open={paying !== null} title={paying?.payslip ? `Pay ${paying.payslip.employeeName}` : 'Pay all salaries'}
        description={paying?.payslip ? `Net ${formatMoney(paying.payslip.netPay, currency)}` : `${run.unpaidPayslips} payslip(s), net ${formatMoney(run.totalNet, currency)} in total`}
        onClose={() => setPaying(null)} busy={pay.isPending}>
        <div className="space-y-4">
          <FormField label="Method" htmlFor="pay-method" required>
            <Select id="pay-method" options={methods.data ?? []} value={method} onChange={(e) => setMethod(e.target.value)} />
          </FormField>
          {method !== 'CASH' && (
            <FormField label="Reference" htmlFor="pay-ref" error={error} hint="Bank batch or cheque number" required>
              <Input id="pay-ref" className="uppercase" invalid={!!error} value={reference} onChange={(e) => setReference(e.target.value)} />
            </FormField>
          )}
          {method === 'CASH' && error && <p className="text-sm text-red-600" role="alert">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setPaying(null)}>Cancel</Button>
            <Button loading={pay.isPending} onClick={submitPayment}>Record payment</Button>
          </div>
        </div>
      </Modal>
    </>
  );
}
