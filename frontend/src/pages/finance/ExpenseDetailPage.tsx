import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, ArrowLeft, Download } from 'lucide-react';
import { expensesApi, pettyCashApi } from '../../api/financeApi';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useEnumLookup } from '../../hooks/useLookup';
import { toApiError } from '../../lib/apiError';
import { formatDate, formatDateTime, formatMoney, humanize } from '../../lib/format';
import { todayIso } from '../../validation/common';
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
import { FullPageSpinner, Spinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Textarea } from '../../components/ui/Textarea';

type Dialog = 'approve' | 'reject' | 'pay' | null;
const REFERENCE = /^[A-Za-z0-9/-]{3,50}$/;

/** One expense: PM approves or rejects; the Accountant pays (bank, cheque, cash or petty cash). */
export default function ExpenseDetailPage() {
  const { id } = useParams();
  const expenseId = Number(id);
  const currency = useCurrency();
  const toast = useToast();
  const queryClient = useQueryClient();
  const methods = useEnumLookup('payment-methods');
  const key = ['expenses', 'detail', expenseId];
  const [dialog, setDialog] = useState<Dialog>(null);
  const [remarks, setRemarks] = useState('');
  const [acknowledge, setAcknowledge] = useState(false);
  const [fromPetty, setFromPetty] = useState(false);
  const [method, setMethod] = useState('BANK_TRANSFER');
  const [reference, setReference] = useState('');
  const [paymentDate, setPaymentDate] = useState(todayIso());
  const [error, setError] = useState<string | undefined>();

  const expense = useQuery({ queryKey: key, queryFn: () => expensesApi.get(expenseId) });
  const budget = useQuery({ queryKey: [...key, 'budget'], queryFn: () => expensesApi.budgetCheck(expenseId), enabled: dialog === 'approve' });
  const fund = useQuery({
    queryKey: ['petty-cash', expense.data?.projectId],
    queryFn: () => pettyCashApi.fund(expense.data!.projectId),
    enabled: dialog === 'pay' && !!expense.data,
    retry: false,
  });

  const open = (d: Dialog) => {
    setDialog(d);
    setRemarks('');
    setAcknowledge(false);
    setFromPetty(false);
    setMethod('BANK_TRANSFER');
    setReference('');
    setPaymentDate(todayIso());
    setError(undefined);
  };

  const act = useMutation({
    mutationFn: async () => {
      if (dialog === 'approve') {
        await expensesApi.approve(expenseId, remarks.trim() || null, acknowledge);
        return 'Expense approved. The accountants have been notified.';
      }
      if (dialog === 'reject') {
        await expensesApi.reject(expenseId, remarks.trim());
        return 'Expense rejected.';
      }
      await expensesApi.pay(expenseId, {
        fromPettyCash: fromPetty,
        method: fromPetty ? 'CASH' : method,
        referenceNo: fromPetty || method === 'CASH' ? null : reference.trim(),
        paymentDate,
        notes: remarks.trim() || null,
      });
      return fromPetty ? 'Expense paid from petty cash.' : 'Expense paid.';
    },
    onSuccess: async (message) => {
      setDialog(null);
      await queryClient.invalidateQueries({ queryKey: ['expenses'] });
      await queryClient.invalidateQueries({ queryKey: ['petty-cash'] });
      toast.success(message);
    },
    onError: (err) => {
      const apiError = toApiError(err);
      setError(Object.values(apiError.fieldErrors)[0] ?? apiError.message);
    },
  });

  const submit = () => {
    setError(undefined);
    if (dialog === 'reject' && remarks.trim().length < 5) {
      setError(CLIENT_MESSAGES.remarksRequired);
      return;
    }
    if (dialog === 'approve' && budget.data?.overrun && !acknowledge) {
      setError('Tick the box to confirm you accept the budget overrun.');
      return;
    }
    if (dialog === 'pay') {
      if (!paymentDate || paymentDate > todayIso()) {
        setError(CLIENT_MESSAGES.dateFuture);
        return;
      }
      if (!fromPetty && method !== 'CASH' && !REFERENCE.test(reference.trim())) {
        setError(CLIENT_MESSAGES.reference);
        return;
      }
    }
    act.mutate();
  };

  if (expense.isLoading) {
    return <FullPageSpinner />;
  }
  if (expense.isError || !expense.data) {
    return <Alert tone="error">{toApiError(expense.error).message}</Alert>;
  }
  const e = expense.data;
  const pettyEnough = fund.data && fund.data.active && Number(fund.data.balance) >= Number(e.amount);

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        Back to expenses
      </Link>
      <PageHeader
        title={`${formatMoney(e.amount, currency)} to ${e.payee}`}
        description={<span className="flex flex-wrap items-center gap-2"><StatusBadge status={e.status} /> {e.projectName}, {e.phaseName}</span>}
        actions={
          <>
            {e.canReview && (
              <>
                <Button variant="secondary" onClick={() => open('reject')}>Reject</Button>
                <Button onClick={() => open('approve')}>Approve</Button>
              </>
            )}
            {e.canPay && <Button onClick={() => open('pay')}>Pay expense</Button>}
          </>
        }
      />
      {e.status === 'REJECTED' && <div className="mb-6"><Alert tone="error">Rejected by {e.approvedByName}: {e.remarks}</Alert></div>}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card title="Expense">
          <DetailList items={[
            { label: 'Date', value: formatDate(e.expenseDate) },
            { label: 'Category', value: humanize(e.category) },
            { label: 'Description', value: e.description },
            { label: 'Submitted by', value: `${e.requestedByName}, ${formatDateTime(e.createdAt)}` },
            {
              label: 'Receipt',
              value: e.receiptName ? (
                <a href={expensesApi.receiptUrl(e.id)} className="inline-flex items-center gap-1 text-primary hover:text-primary-dark">
                  <Download className="h-4 w-4" aria-hidden />{e.receiptName}
                </a>
              ) : e.receiptRequired ? 'Missing' : 'Not needed',
            },
          ]} />
        </Card>
        <Card title="Progress">
          <DetailList items={[
            { label: 'Decision', value: e.decidedAt ? `${e.approvedByName}, ${formatDateTime(e.decidedAt)}` : 'Waiting for the project manager' },
            ...(e.remarks && e.status !== 'REJECTED' ? [{ label: 'PM remarks', value: e.remarks }] : []),
            {
              label: 'Payment',
              value: e.paidAt
                ? `${formatDateTime(e.paidAt)}, ${e.paidFromPettyCash ? 'petty cash' : humanize(e.paymentMethod ?? '')}${e.paymentReference ? ` (${e.paymentReference})` : ''}`
                : e.status === 'APPROVED' ? 'Waiting for the accountant' : '-',
            },
          ]} />
        </Card>
      </div>

      <Modal open={dialog !== null} title={{ approve: 'Approve expense', reject: 'Reject expense', pay: 'Pay expense' }[dialog ?? 'approve']}
        onClose={() => setDialog(null)} busy={act.isPending}>
        <div className="space-y-4">
          {dialog === 'approve' && (budget.isLoading ? <Spinner /> : budget.data && (
            <div className={`rounded-lg border p-4 text-sm ${budget.data.overrun ? 'border-red-200 bg-red-50 text-red-800' : 'border-line bg-surface'}`}>
              <p className="font-medium">{budget.data.phaseName} budget</p>
              <p className="mt-1">Budget {formatMoney(budget.data.phaseBudget, currency)}, committed {formatMoney(budget.data.committed, currency)}, left {formatMoney(budget.data.remaining, currency)}.</p>
              {budget.data.overrun && (
                <label className="mt-3 flex items-start gap-2">
                  <input type="checkbox" className="mt-0.5 accent-red-600" checked={acknowledge} onChange={(ev) => setAcknowledge(ev.target.checked)} />
                  <span className="flex items-center gap-1"><AlertTriangle className="h-4 w-4" aria-hidden />I accept that this exceeds the phase budget.</span>
                </label>
              )}
            </div>
          ))}
          {dialog === 'pay' && (
            <>
              <label className={`flex items-start gap-2 text-sm ${pettyEnough ? 'text-ink' : 'text-muted'}`}>
                <input type="checkbox" className="mt-0.5 accent-primary" disabled={!pettyEnough} checked={fromPetty} onChange={(ev) => setFromPetty(ev.target.checked)} />
                <span>
                  Pay from the project's petty cash
                  <span className="block text-xs text-muted">
                    {fund.isLoading ? 'Checking the fund' : fund.data ? `Balance ${formatMoney(fund.data.balance, currency)}${pettyEnough ? '' : ', not enough'}` : 'This project has no petty cash fund.'}
                  </span>
                </span>
              </label>
              {!fromPetty && (
                <div className="grid gap-4 sm:grid-cols-2">
                  <FormField label="Method" htmlFor="pay-method" required>
                    <Select id="pay-method" options={methods.data ?? []} value={method} onChange={(ev) => setMethod(ev.target.value)} />
                  </FormField>
                  {method !== 'CASH' && (
                    <FormField label="Reference" htmlFor="pay-ref" required>
                      <Input id="pay-ref" className="uppercase" value={reference} onChange={(ev) => setReference(ev.target.value)} />
                    </FormField>
                  )}
                </div>
              )}
              <FormField label="Payment date" htmlFor="pay-date" required>
                <Input id="pay-date" type="date" max={todayIso()} value={paymentDate} onChange={(ev) => setPaymentDate(ev.target.value)} />
              </FormField>
            </>
          )}
          <FormField label={dialog === 'reject' ? 'Reason' : dialog === 'pay' ? 'Notes (optional)' : 'Remarks (optional)'} htmlFor="ex-remarks" required={dialog === 'reject'}>
            <Textarea id="ex-remarks" rows={3} maxLength={500} value={remarks} onChange={(ev) => setRemarks(ev.target.value)} />
          </FormField>
          {error && <p className="text-sm text-red-600" role="alert">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDialog(null)} disabled={act.isPending}>Cancel</Button>
            <Button variant={dialog === 'reject' ? 'danger' : 'primary'} loading={act.isPending} onClick={submit}>
              {{ approve: 'Approve', reject: 'Reject expense', pay: `Pay ${formatMoney(e.amount, currency)}` }[dialog ?? 'approve']}
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}
