import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Download, Pencil, Send, Trash2 } from 'lucide-react';
import { clientInvoicesApi } from '../../api/financeApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useEnumLookup } from '../../hooks/useLookup';
import { toApiError } from '../../lib/apiError';
import { formatDate, formatDateTime, formatMoney, humanize } from '../../lib/format';
import { PATTERNS, todayIso } from '../../validation/common';
import { CLIENT_MESSAGES } from '../../validation/messages';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { DetailList } from '../../components/ui/DetailList';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { Select } from '../../components/ui/Select';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';

/** One client invoice: issue a draft, record receipts, print the PDF. */
export default function ClientInvoiceDetailPage() {
  const { id } = useParams();
  const invoiceId = Number(id);
  const { hasPermission } = useAuth();
  const canEdit = hasPermission('INVOICE:EDIT');
  const navigate = useNavigate();
  const currency = useCurrency();
  const toast = useToast();
  const queryClient = useQueryClient();
  const methods = useEnumLookup('payment-methods');
  const key = ['client-invoices', 'detail', invoiceId];
  const [confirm, setConfirm] = useState<'issue' | 'delete' | null>(null);
  const [receiving, setReceiving] = useState(false);
  const [amount, setAmount] = useState('');
  const [method, setMethod] = useState('BANK_TRANSFER');
  const [reference, setReference] = useState('');
  const [date, setDate] = useState(todayIso());
  const [error, setError] = useState<string | undefined>();

  const invoice = useQuery({ queryKey: key, queryFn: () => clientInvoicesApi.get(invoiceId) });
  const refresh = async (message: string) => {
    await queryClient.invalidateQueries({ queryKey: ['client-invoices'] });
    toast.success(message);
  };
  const issue = useMutation({
    mutationFn: () => clientInvoicesApi.issue(invoiceId),
    onSuccess: async (i) => { setConfirm(null); await refresh(`${i.invoiceNumber} issued to ${i.clientName}.`); },
    onError: (err) => { setConfirm(null); toast.error(toApiError(err).message); },
  });
  const remove = useMutation({
    mutationFn: () => clientInvoicesApi.remove(invoiceId),
    onSuccess: async () => { await refresh('Draft deleted.'); navigate('..', { relative: 'path', replace: true }); },
    onError: (err) => { setConfirm(null); toast.error(toApiError(err).message); },
  });
  const receive = useMutation({
    mutationFn: () => clientInvoicesApi.receive(invoiceId, { amount: amount.trim(), paymentDate: date, method, referenceNo: method === 'CASH' ? null : reference.trim(), notes: null }),
    onSuccess: async (i) => { setReceiving(false); await refresh(i.status === 'PAID' ? 'Invoice paid in full.' : `Payment recorded. ${formatMoney(i.outstanding, currency)} still owed.`); },
    onError: (err) => { const e = toApiError(err); setError(Object.values(e.fieldErrors)[0] ?? e.message); },
  });

  if (invoice.isLoading) {
    return <FullPageSpinner />;
  }
  if (invoice.isError || !invoice.data) {
    return <Alert tone="error">{toApiError(invoice.error).message}</Alert>;
  }
  const i = invoice.data;
  const draft = i.status === 'DRAFT';
  const open = !draft && i.status !== 'PAID';

  const submitReceipt = () => {
    setError(undefined);
    if (!PATTERNS.money.test(amount.trim()) || Number(amount) <= 0) { setError(CLIENT_MESSAGES.moneyPositive); return; }
    if (Number(amount) > Number(i.outstanding)) { setError(CLIENT_MESSAGES.overpay); return; }
    if (!date || date > todayIso()) { setError(CLIENT_MESSAGES.dateFuture); return; }
    if (method !== 'CASH' && !/^[A-Za-z0-9/-]{3,50}$/.test(reference.trim())) { setError(CLIENT_MESSAGES.reference); return; }
    receive.mutate();
  };

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />Back to invoices
      </Link>
      <PageHeader
        title={i.invoiceNumber}
        description={<span className="flex flex-wrap items-center gap-2"><StatusBadge status={i.status} />{i.clientName}, {i.projectName}</span>}
        actions={
          <>
            <a href={clientInvoicesApi.pdfUrl(i.id)} className="inline-flex h-11 items-center gap-2 rounded-lg border border-line bg-white px-4 text-sm font-medium text-ink hover:bg-surface">
              <Download className="h-4 w-4" aria-hidden />PDF
            </a>
            {canEdit && draft && (
              <>
                <Button variant="secondary" onClick={() => setConfirm('delete')}><Trash2 className="h-4 w-4" aria-hidden />Delete</Button>
                <Link to="edit" className="inline-flex h-11 items-center gap-2 rounded-lg border border-line bg-white px-4 text-sm font-medium text-ink hover:bg-surface">
                  <Pencil className="h-4 w-4" aria-hidden />Edit
                </Link>
                <Button onClick={() => setConfirm('issue')}><Send className="h-4 w-4" aria-hidden />Issue</Button>
              </>
            )}
            {canEdit && open && (
              <Button onClick={() => { setReceiving(true); setAmount(Number(i.outstanding).toFixed(2)); setReference(''); setError(undefined); }}>Record payment</Button>
            )}
          </>
        }
      />
      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Lines" className="lg:col-span-2">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[480px] text-sm">
              <thead className="text-left text-muted">
                <tr className="border-b border-line">
                  <th scope="col" className="py-2 pr-3 font-medium">Description</th>
                  <th scope="col" className="py-2 pr-3 text-right font-medium">Qty</th>
                  <th scope="col" className="py-2 pr-3 text-right font-medium">Unit price</th>
                  <th scope="col" className="py-2 text-right font-medium">Amount</th>
                </tr>
              </thead>
              <tbody>
                {i.lines.map((l, idx) => (
                  <tr key={idx} className="border-b border-line">
                    <td className="py-2 pr-3">{l.description}</td>
                    <td className="py-2 pr-3 text-right">{Number(l.quantity)}</td>
                    <td className="py-2 pr-3 text-right">{formatMoney(l.unitPrice, currency)}</td>
                    <td className="py-2 text-right">{formatMoney(l.lineTotal, currency)}</td>
                  </tr>
                ))}
                <tr><td colSpan={3} className="py-1.5 pr-3 text-right text-muted">Subtotal</td><td className="py-1.5 text-right">{formatMoney(i.subtotal, currency)}</td></tr>
                <tr><td colSpan={3} className="py-1.5 pr-3 text-right text-muted">Tax ({Number(i.taxRate)}%)</td><td className="py-1.5 text-right">{formatMoney(i.taxAmount, currency)}</td></tr>
                <tr><td colSpan={3} className="py-1.5 pr-3 text-right font-semibold">Total</td><td className="py-1.5 text-right font-semibold">{formatMoney(i.total, currency)}</td></tr>
              </tbody>
            </table>
          </div>
          {i.notes && <p className="mt-4 text-sm text-muted">Notes: {i.notes}</p>}
        </Card>
        <Card title="Summary">
          <DetailList items={[
            { label: 'Invoice date', value: formatDate(i.issueDate) },
            { label: 'Due date', value: formatDate(i.dueDate) },
            { label: 'Issued', value: i.issuedAt ? formatDateTime(i.issuedAt) : 'Draft' },
            { label: 'Paid', value: formatMoney(i.paidAmount, currency) },
            { label: 'Still owed', value: <span className="font-semibold">{formatMoney(i.outstanding, currency)}</span> },
          ]} />
        </Card>
      </div>
      <Card title="Payments received" className="mt-6">
        {i.receipts.length === 0 ? <p className="text-sm text-muted">No payments yet.</p> : (
          <ul className="divide-y divide-line text-sm">
            {i.receipts.map((r) => (
              <li key={r.id} className="flex flex-wrap justify-between gap-2 py-2">
                <span>{formatDate(r.paymentDate)}, {humanize(r.method)}{r.referenceNo ? ` ${r.referenceNo}` : ''} <span className="text-muted">by {r.receivedByName}</span></span>
                <span className="font-medium">{formatMoney(r.amount, currency)}</span>
              </li>
            ))}
          </ul>
        )}
      </Card>

      <ConfirmDialog open={confirm === 'issue'} title={`Issue ${i.invoiceNumber}?`} message={`The invoice of ${formatMoney(i.total, currency)} can no longer be edited after it is issued.`}
        confirmLabel="Issue invoice" loading={issue.isPending} onConfirm={() => issue.mutate()} onCancel={() => setConfirm(null)} />
      <ConfirmDialog open={confirm === 'delete'} title={`Delete draft ${i.invoiceNumber}?`} message="The draft and its lines are removed. The number is not reused."
        confirmLabel="Delete draft" tone="danger" loading={remove.isPending} onConfirm={() => remove.mutate()} onCancel={() => setConfirm(null)} />
      <Modal open={receiving} title="Record payment" description={`Still owed: ${formatMoney(i.outstanding, currency)}`} onClose={() => setReceiving(false)} busy={receive.isPending}>
        <div className="space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField label={`Amount (${currency})`} htmlFor="rc-amount" required><Input id="rc-amount" inputMode="decimal" value={amount} onChange={(e) => setAmount(e.target.value)} /></FormField>
            <FormField label="Date received" htmlFor="rc-date" required><Input id="rc-date" type="date" max={todayIso()} value={date} onChange={(e) => setDate(e.target.value)} /></FormField>
            <FormField label="Method" htmlFor="rc-method" required><Select id="rc-method" options={methods.data ?? []} value={method} onChange={(e) => setMethod(e.target.value)} /></FormField>
            {method !== 'CASH' && <FormField label="Reference" htmlFor="rc-ref" required><Input id="rc-ref" className="uppercase" value={reference} onChange={(e) => setReference(e.target.value)} /></FormField>}
          </div>
          {error && <p className="text-sm text-red-600" role="alert">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setReceiving(false)}>Cancel</Button>
            <Button loading={receive.isPending} onClick={submitReceipt}>Record payment</Button>
          </div>
        </div>
      </Modal>
    </>
  );
}
