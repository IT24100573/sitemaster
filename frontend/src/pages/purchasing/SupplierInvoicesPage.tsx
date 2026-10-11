import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { purchasingApi, supplierPortalApi } from '../../api/purchasingApi';
import { useAuth, useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useEnumLookup } from '../../hooks/useLookup';
import { useTableState } from '../../hooks/useTableState';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { formatDate, formatMoney } from '../../lib/format';
import { emptyToNull } from '../../lib/params';
import { todayIso } from '../../validation/common';
import { CLIENT_MESSAGES } from '../../validation/messages';
import { paymentSchema, type PaymentFormValues } from '../../validation/siteSchemas';
import { Button } from '../../components/ui/Button';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Textarea } from '../../components/ui/Textarea';
import type { SupplierInvoice } from '../../types/purchase';

const PAYMENT_FIELDS = ['amount', 'method', 'referenceNo', 'paymentDate', 'notes'] as const;
const SUPPLIER_INVOICE_STATUSES = ['ISSUED', 'PARTIALLY_PAID', 'PAID'];

function PaymentForm({ invoice, onDone }: { invoice: SupplierInvoice; onDone: () => void }) {
  const toast = useToast();
  const currency = useCurrency();
  const queryClient = useQueryClient();
  const methods = useEnumLookup('payment-methods');
  const { register, handleSubmit, setError, watch, formState: { errors, isSubmitting, submitCount } } = useForm<PaymentFormValues>({
    resolver: zodResolver(paymentSchema),
    mode: 'onBlur',
    defaultValues: { amount: Number(invoice.outstanding).toFixed(2), method: 'BANK_TRANSFER', referenceNo: '', paymentDate: todayIso(), notes: '' },
  });
  const feedback = useFormFeedback(setError, PAYMENT_FIELDS);
  const method = watch('method');

  const onSubmit = async (v: PaymentFormValues) => {
    feedback.clear();
    if (Number(v.amount) > Number(invoice.outstanding)) {
      setError('amount', { type: 'max', message: CLIENT_MESSAGES.overpay });
      return;
    }
    try {
      const updated = await purchasingApi.pay(invoice.id, {
        amount: v.amount,
        method: v.method,
        referenceNo: v.method === 'CASH' ? null : emptyToNull(v.referenceNo),
        paymentDate: v.paymentDate,
        notes: emptyToNull(v.notes),
      });
      await queryClient.invalidateQueries({ queryKey: ['supplier-invoices'] });
      await queryClient.invalidateQueries({ queryKey: ['purchase-orders'] });
      await queryClient.invalidateQueries({ queryKey: ['payments'] });
      toast.success(updated.status === 'PAID' ? `Invoice ${updated.invoiceNumber} paid in full. The site engineer has been asked to confirm delivery.` : `Part payment recorded. ${formatMoney(updated.outstanding, currency)} still owed.`);
      onDone();
    } catch (error) {
      feedback.handleError(error);
    }
  };
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <p className="text-sm text-muted">
        {invoice.supplierName}, invoice {invoice.invoiceNumber} for {invoice.poNumber}. Still owed {formatMoney(invoice.outstanding, currency)}.
      </p>
      <ErrorSummary messages={summary} />
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label={`Amount (${currency})`} htmlFor="pay-amount" error={errors.amount?.message} required>
          <Input id="pay-amount" inputMode="decimal" invalid={!!errors.amount} {...register('amount')} />
        </FormField>
        <FormField label="Payment date" htmlFor="pay-date" error={errors.paymentDate?.message} required>
          <Input id="pay-date" type="date" max={todayIso()} invalid={!!errors.paymentDate} {...register('paymentDate')} />
        </FormField>
        <FormField label="Method" htmlFor="pay-method" error={errors.method?.message} required>
          <Select id="pay-method" options={methods.data ?? []} invalid={!!errors.method} {...register('method')} />
        </FormField>
        {method !== 'CASH' && (
          <FormField label="Reference number" htmlFor="pay-reference" error={errors.referenceNo?.message} required>
            <Input id="pay-reference" className="uppercase" invalid={!!errors.referenceNo} {...register('referenceNo')} />
          </FormField>
        )}
      </div>
      <FormField label="Notes" htmlFor="pay-notes" error={errors.notes?.message}>
        <Textarea id="pay-notes" rows={2} maxLength={500} {...register('notes')} />
      </FormField>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={isSubmitting}>Record payment</Button>
      </div>
    </form>
  );
}

/** Supplier invoices: the Accountant pays them; suppliers track their own. */
export default function SupplierInvoicesPage() {
  const user = useCurrentUser();
  const isSupplier = user.role === 'SUPPLIER';
  const { hasPermission } = useAuth();
  const currency = useCurrency();
  const table = useTableState('createdAt', 'desc');
  const [status, setStatus] = useState('');
  const [paying, setPaying] = useState<SupplierInvoice | null>(null);
  const statuses = useEnumLookup('invoice-statuses');

  const invoices = useQuery({
    queryKey: ['supplier-invoices', table.params, status, isSupplier],
    queryFn: () => (isSupplier ? supplierPortalApi.invoices(table.params, status || undefined) : purchasingApi.invoices(table.params, status || undefined)),
    placeholderData: keepPreviousData,
  });

  const columns: Column<SupplierInvoice>[] = [
    { key: 'number', header: 'Invoice', render: (i) => (
      <div>
        <p className="font-medium text-ink">{i.invoiceNumber}</p>
        <p className="text-xs text-muted">{i.poNumber}{isSupplier ? '' : `, ${i.supplierName}`}</p>
      </div>
    ) },
    { key: 'project', header: 'Project', render: (i) => i.projectName },
    { key: 'date', header: 'Date', sortKey: 'invoiceDate', className: 'whitespace-nowrap', render: (i) => formatDate(i.invoiceDate) },
    { key: 'amount', header: 'Amount', sortKey: 'amount', className: 'whitespace-nowrap text-right', render: (i) => formatMoney(i.amount, currency) },
    { key: 'owed', header: 'Owed', className: 'whitespace-nowrap text-right', render: (i) => formatMoney(i.outstanding, currency) },
    { key: 'status', header: 'Status', sortKey: 'status', render: (i) => <StatusBadge status={i.status} /> },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (i) => (
        <div className="flex items-center justify-end gap-1">
          <a href={isSupplier ? supplierPortalApi.invoiceFileUrl(i.id) : purchasingApi.invoiceFileUrl(i.id)} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Download invoice ${i.invoiceNumber}`} title="Download">
            <Download className="h-4 w-4" aria-hidden />
          </a>
          {i.payable && hasPermission('PAYMENT:CREATE') && (
            <Button size="sm" onClick={() => setPaying(i)}>Pay</Button>
          )}
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader
        title={isSupplier ? 'Invoices' : 'Supplier invoices'}
        description={isSupplier ? 'Invoices you uploaded and how much has been paid.' : 'Invoices from suppliers. Pay invoiced orders here.'}
      />
      <DataTable
        columns={columns}
        page={invoices.data}
        rowKey={(i) => i.id}
        loading={invoices.isFetching}
        error={invoices.isError ? toApiError(invoices.error).message : null}
        emptyMessage="No invoices match these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Invoice number" />
            <div className="sm:ml-auto">
              <Select
                aria-label="Filter by status"
                options={(statuses.data ?? []).filter((s) => SUPPLIER_INVOICE_STATUSES.includes(s.value))}
                placeholder="Any status"
                value={status}
                onChange={(e) => { setStatus(e.target.value); table.resetPage(); }}
              />
            </div>
          </>
        }
      />
      <Modal open={paying !== null} title="Record payment" onClose={() => setPaying(null)}>
        {paying && <PaymentForm invoice={paying} onDone={() => setPaying(null)} />}
      </Modal>
    </>
  );
}
