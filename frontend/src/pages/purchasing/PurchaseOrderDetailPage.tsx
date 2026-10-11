import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { purchasingApi, supplierPortalApi } from '../../api/purchasingApi';
import { useAuth, useCurrentUser } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { formatDate, formatDateTime, formatMoney } from '../../lib/format';
import { todayIso } from '../../validation/common';
import { CLIENT_MESSAGES } from '../../validation/messages';
import { invoiceUploadSchema, type InvoiceUploadFormValues } from '../../validation/siteSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { DetailList } from '../../components/ui/DetailList';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FileUpload } from '../../components/ui/FileUpload';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Textarea } from '../../components/ui/Textarea';
import type { PurchaseOrder } from '../../types/purchase';

const INVOICE_FIELDS = ['invoiceNumber', 'invoiceDate', 'amount'] as const;

function InvoiceForm({ order, onDone }: { order: PurchaseOrder; onDone: (updated: PurchaseOrder) => void }) {
  const currency = useCurrency();
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | undefined>();
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } = useForm<InvoiceUploadFormValues>({
    resolver: zodResolver(invoiceUploadSchema),
    mode: 'onBlur',
    defaultValues: { invoiceNumber: '', invoiceDate: todayIso(), amount: Number(order.totalAmount).toFixed(2) },
  });
  const feedback = useFormFeedback(setError, INVOICE_FIELDS);
  const onSubmit = async (v: InvoiceUploadFormValues) => {
    feedback.clear();
    setFileError(undefined);
    if (!file) {
      setFileError(CLIENT_MESSAGES.fileRequired);
      return;
    }
    if (Number(v.amount) > Number(order.totalAmount)) {
      setError('amount', { type: 'max', message: `Invoice amount cannot be more than the order total of ${formatMoney(order.totalAmount, currency)}.` });
      return;
    }
    try {
      onDone(await supplierPortalApi.uploadInvoice(order.id, v.invoiceNumber, v.invoiceDate, v.amount, file));
    } catch (error) {
      setFileError(toApiError(error).fieldErrors.file);
      feedback.handleError(error);
    }
  };
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];
  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Invoice number" htmlFor="inv-number" error={errors.invoiceNumber?.message} required>
          <Input id="inv-number" className="uppercase" invalid={!!errors.invoiceNumber} {...register('invoiceNumber')} />
        </FormField>
        <FormField label="Invoice date" htmlFor="inv-date" error={errors.invoiceDate?.message} required>
          <Input id="inv-date" type="date" max={todayIso()} invalid={!!errors.invoiceDate} {...register('invoiceDate')} />
        </FormField>
      </div>
      <FormField label={`Amount (${currency})`} htmlFor="inv-amount" error={errors.amount?.message} hint={`Order total: ${formatMoney(order.totalAmount, currency)}`} required>
        <Input id="inv-amount" inputMode="decimal" invalid={!!errors.amount} {...register('amount')} />
      </FormField>
      <FileUpload kind="document" file={file} onChange={(f) => { setFile(f); setFileError(undefined); }} label="Attach the invoice (PDF)" error={fileError} />
      <div className="flex justify-end">
        <Button type="submit" loading={isSubmitting}>Upload invoice</Button>
      </div>
    </form>
  );
}

type Dialog = 'decline' | 'dispatch' | 'invoice' | null;

/** One purchase order. Suppliers act on it here; staff follow its progress. */
export default function PurchaseOrderDetailPage() {
  const { id } = useParams();
  const orderId = Number(id);
  const user = useCurrentUser();
  const isSupplier = user.role === 'SUPPLIER';
  const { hasPermission } = useAuth();
  const currency = useCurrency();
  const toast = useToast();
  const queryClient = useQueryClient();
  const key = ['purchase-orders', 'detail', orderId];
  const [dialog, setDialog] = useState<Dialog>(null);
  const [reason, setReason] = useState('');
  const [expected, setExpected] = useState(todayIso());
  const [error, setError] = useState<string | undefined>();

  const order = useQuery({
    queryKey: key,
    queryFn: () => (isSupplier ? supplierPortalApi.order(orderId) : purchasingApi.order(orderId)),
  });

  const saved = async (updated: PurchaseOrder, message: string) => {
    queryClient.setQueryData(key, updated);
    await queryClient.invalidateQueries({ queryKey: ['purchase-orders'] });
    setDialog(null);
    toast.success(message);
  };

  const act = useMutation({
    mutationFn: async (kind: 'accept' | 'decline' | 'dispatch') => {
      if (kind === 'accept') {
        return { order: await supplierPortalApi.accept(orderId), message: 'Order accepted.' };
      }
      if (kind === 'decline') {
        return { order: await supplierPortalApi.decline(orderId, reason.trim()), message: 'Order declined.' };
      }
      return { order: await supplierPortalApi.dispatch(orderId, expected), message: 'Order marked as dispatched.' };
    },
    onSuccess: ({ order: updated, message }) => saved(updated, message),
    onError: (err) => {
      const apiError = toApiError(err);
      const message = Object.values(apiError.fieldErrors)[0] ?? apiError.message;
      if (dialog) {
        setError(message);
      } else {
        toast.error(message);
      }
    },
  });

  if (order.isLoading) {
    return <FullPageSpinner />;
  }
  if (order.isError || !order.data) {
    return <Alert tone="error">{toApiError(order.error).message}</Alert>;
  }
  const o = order.data;
  const canEdit = isSupplier && hasPermission('PURCHASE_ORDER:EDIT');
  const canDispatch = canEdit && (o.status === 'ACCEPTED' || (o.status === 'INVOICED' && !o.dispatchedAt));
  const canInvoice = isSupplier && hasPermission('SUPPLIER_INVOICE:CREATE') && (o.status === 'ACCEPTED' || o.status === 'DISPATCHED');

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        Back to purchase orders
      </Link>
      <PageHeader
        title={o.poNumber}
        description={<span className="flex flex-wrap items-center gap-2"><StatusBadge status={o.status} /> {isSupplier ? `Deliver to ${o.projectName}` : o.supplierName}</span>}
        actions={
          <>
            {canEdit && o.status === 'SENT' && (
              <>
                <Button variant="secondary" onClick={() => { setDialog('decline'); setReason(''); setError(undefined); }}>Decline</Button>
                <Button loading={act.isPending && !dialog} onClick={() => act.mutate('accept')}>Accept order</Button>
              </>
            )}
            {canDispatch && <Button variant="secondary" onClick={() => { setDialog('dispatch'); setError(undefined); }}>Mark dispatched</Button>}
            {canInvoice && <Button onClick={() => setDialog('invoice')}>Upload invoice</Button>}
          </>
        }
      />

      {o.status === 'DECLINED' && <div className="mb-6"><Alert tone="error">Declined: {o.declineReason}</Alert></div>}

      <div className="grid gap-6 lg:grid-cols-3">
        <Card title="Items" className="lg:col-span-2">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[480px] text-sm">
              <thead className="text-left text-muted">
                <tr className="border-b border-line">
                  <th scope="col" className="py-2 pr-4 font-medium">Material</th>
                  <th scope="col" className="py-2 pr-4 text-right font-medium">Quantity</th>
                  <th scope="col" className="py-2 pr-4 text-right font-medium">Unit price</th>
                  <th scope="col" className="py-2 text-right font-medium">Total</th>
                </tr>
              </thead>
              <tbody>
                {o.items.map((item) => (
                  <tr key={item.materialName} className="border-b border-line">
                    <td className="py-2.5 pr-4 text-ink">{item.materialName}</td>
                    <td className="py-2.5 pr-4 text-right">{item.quantity} {item.unit}</td>
                    <td className="py-2.5 pr-4 text-right">{formatMoney(item.unitPrice, currency)}</td>
                    <td className="py-2.5 text-right">{formatMoney(item.lineTotal, currency)}</td>
                  </tr>
                ))}
                <tr>
                  <td colSpan={3} className="py-2.5 pr-4 text-right font-semibold text-ink">Order total</td>
                  <td className="py-2.5 text-right font-semibold text-ink">{formatMoney(o.totalAmount, currency)}</td>
                </tr>
              </tbody>
            </table>
          </div>
          {o.notes && <p className="mt-4 text-sm text-muted">Notes: {o.notes}</p>}
        </Card>
        <Card title="Order">
          <DetailList
            items={[
              { label: 'Ordered', value: formatDate(o.orderDate) },
              { label: 'Supplier', value: o.supplierName },
              { label: 'Delivery site', value: `${o.projectName}, ${o.deliveryLocation}` },
              ...(o.respondedAt ? [{ label: 'Answered', value: formatDateTime(o.respondedAt) }] : []),
              ...(o.dispatchedAt ? [{ label: 'Dispatched', value: `${formatDateTime(o.dispatchedAt)}, expected ${formatDate(o.expectedDeliveryDate)}` }] : []),
              ...(o.invoice ? [{ label: 'Invoice', value: `${o.invoice.invoiceNumber}, ${formatMoney(o.invoice.amount, currency)} (${o.invoice.status.toLowerCase().replace('_', ' ')})` }] : []),
              ...(o.paidAt ? [{ label: 'Paid', value: formatDateTime(o.paidAt) }] : []),
              ...(o.deliveredAt ? [{ label: 'Delivered', value: formatDateTime(o.deliveredAt) }] : []),
            ]}
          />
          {!isSupplier && (
            <Link to={`../../material-requests/${o.materialRequestId}`} relative="path" className="mt-4 inline-block text-sm font-medium text-primary hover:text-primary-dark">
              View the material request
            </Link>
          )}
        </Card>
      </div>

      <Modal open={dialog === 'decline'} title="Decline this order?" onClose={() => setDialog(null)} busy={act.isPending}>
        <div className="space-y-4">
          <FormField label="Reason" htmlFor="decline-reason" error={error} required>
            <Textarea id="decline-reason" rows={3} maxLength={500} invalid={!!error} value={reason} onChange={(e) => setReason(e.target.value)} />
          </FormField>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDialog(null)}>Cancel</Button>
            <Button
              variant="danger"
              loading={act.isPending}
              onClick={() => (reason.trim().length < 5 ? setError(CLIENT_MESSAGES.reason) : act.mutate('decline'))}
            >
              Decline order
            </Button>
          </div>
        </div>
      </Modal>
      <Modal open={dialog === 'dispatch'} title="Mark as dispatched" onClose={() => setDialog(null)} busy={act.isPending}>
        <div className="space-y-4">
          <FormField label="Expected delivery date" htmlFor="expected" error={error} required>
            <Input id="expected" type="date" min={todayIso()} invalid={!!error} value={expected} onChange={(e) => setExpected(e.target.value)} />
          </FormField>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDialog(null)}>Cancel</Button>
            <Button
              loading={act.isPending}
              onClick={() => (!expected || expected < todayIso() ? setError('Expected delivery date must be today or later.') : act.mutate('dispatch'))}
            >
              Mark dispatched
            </Button>
          </div>
        </div>
      </Modal>
      <Modal open={dialog === 'invoice'} title={`Invoice for ${o.poNumber}`} onClose={() => setDialog(null)}>
        {dialog === 'invoice' && <InvoiceForm order={o} onDone={(updated) => saved(updated, 'Invoice uploaded. The accountants have been notified.')} />}
      </Modal>
    </>
  );
}
