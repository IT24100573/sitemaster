import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertTriangle, ArrowLeft, Check } from 'lucide-react';
import { materialRequestsApi } from '../../api/materialRequestsApi';
import { suppliersApi } from '../../api/suppliersApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { toApiError } from '../../lib/apiError';
import { formatDate, formatDateTime, formatMoney } from '../../lib/format';
import { CLIENT_MESSAGES } from '../../validation/messages';
import { PATTERNS } from '../../validation/common';
import { QUANTITY } from '../../validation/siteSchemas';
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
import type { MaterialRequest } from '../../types/materialRequest';

const STEPS: { status: MaterialRequest['status']; label: string }[] = [
  { status: 'PENDING', label: 'Requested' },
  { status: 'APPROVED', label: 'Approved by PM' },
  { status: 'ORDERED', label: 'Ordered from supplier' },
  { status: 'PAID', label: 'Paid' },
  { status: 'DELIVERED', label: 'Delivered to site' },
];

type Dialog = 'approve' | 'reject' | 'order' | 'deliver' | null;

/** One material request with the action each role can take next. */
export default function MaterialRequestDetailPage() {
  const { id } = useParams();
  const requestId = Number(id);
  const { hasPermission } = useAuth();
  const currency = useCurrency();
  const toast = useToast();
  const queryClient = useQueryClient();
  const key = ['material-requests', 'detail', requestId];
  const [dialog, setDialog] = useState<Dialog>(null);
  const [remarks, setRemarks] = useState('');
  const [acknowledge, setAcknowledge] = useState(false);
  const [supplierId, setSupplierId] = useState('');
  const [unitPrice, setUnitPrice] = useState('');
  const [quantity, setQuantity] = useState('');
  const [error, setError] = useState<string | undefined>();

  const request = useQuery({ queryKey: key, queryFn: () => materialRequestsApi.get(requestId) });
  const budget = useQuery({
    queryKey: [...key, 'budget'],
    queryFn: () => materialRequestsApi.budgetCheck(requestId),
    enabled: dialog === 'approve',
  });
  const suppliers = useQuery({
    queryKey: ['suppliers', 'options'],
    queryFn: () => suppliersApi.list({ page: 0, size: 100, sort: 'name', direction: 'asc' }, 'true'),
    enabled: dialog === 'order',
  });

  const open = (d: Dialog) => {
    setDialog(d);
    setRemarks('');
    setAcknowledge(false);
    setError(undefined);
    if (d === 'order' && request.data) {
      setUnitPrice((Number(request.data.estimatedCost) / Number(request.data.quantity)).toFixed(2));
    }
    if (d === 'deliver' && request.data) {
      setQuantity(String(request.data.quantity));
    }
  };

  const action = useMutation({
    mutationFn: async (): Promise<string> => {
      switch (dialog) {
        case 'approve':
          await materialRequestsApi.approve(requestId, remarks.trim() || null, acknowledge);
          return 'Request approved. The accountants have been notified.';
        case 'reject':
          await materialRequestsApi.reject(requestId, remarks.trim());
          return 'Request rejected.';
        case 'order': {
          const po = await materialRequestsApi.raisePurchaseOrder(requestId, Number(supplierId), unitPrice.trim(), remarks.trim() || null);
          return `Purchase order ${po.poNumber} sent to ${po.supplierName}.`;
        }
        default:
          await materialRequestsApi.confirmDelivery(requestId, quantity.trim(), remarks.trim() || null);
          return 'Delivery confirmed and site stock updated.';
      }
    },
    onSuccess: async (message) => {
      setDialog(null);
      await queryClient.invalidateQueries({ queryKey: ['material-requests'] });
      await queryClient.invalidateQueries({ queryKey: ['purchase-orders'] });
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
    if (dialog === 'order' && (!supplierId || !PATTERNS.money.test(unitPrice.trim()) || Number(unitPrice) <= 0)) {
      setError(!supplierId ? 'Choose a supplier.' : CLIENT_MESSAGES.moneyPositive);
      return;
    }
    if (dialog === 'deliver') {
      const value = quantity.trim();
      if (!QUANTITY.test(value) || Number(value) <= 0 || Number(value) > Number(request.data?.quantity)) {
        setError(`Enter a quantity above zero and at most ${request.data?.quantity}.`);
        return;
      }
    }
    action.mutate();
  };

  if (request.isLoading) {
    return <FullPageSpinner />;
  }
  if (request.isError || !request.data) {
    return <Alert tone="error">{toApiError(request.error).message}</Alert>;
  }
  const r = request.data;
  const canOrder = r.status === 'APPROVED' && hasPermission('PURCHASE_ORDER:CREATE');
  const reached = r.status === 'REJECTED' ? 0 : STEPS.findIndex((s) => s.status === r.status);

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        Back to requests
      </Link>
      <PageHeader
        title={`${Number(r.quantity).toLocaleString('en-LK', { maximumFractionDigits: 3 })} ${r.unit} ${r.materialName}`}
        description={<span className="flex flex-wrap items-center gap-2"><StatusBadge status={r.status} /> {r.projectName}, {r.phaseName}</span>}
        actions={
          <>
            {r.canReview && (
              <>
                <Button variant="secondary" onClick={() => open('reject')}>Reject</Button>
                <Button onClick={() => open('approve')}>Approve</Button>
              </>
            )}
            {canOrder && <Button onClick={() => open('order')}>Raise purchase order</Button>}
            {r.canConfirmDelivery && <Button onClick={() => open('deliver')}>Confirm delivery</Button>}
          </>
        }
      />

      {r.status === 'REJECTED' ? (
        <div className="mb-6"><Alert tone="error">Rejected by {r.approvedByName}: {r.remarks}</Alert></div>
      ) : (
        <ol className="mb-6 grid gap-2 sm:grid-cols-5" aria-label="Request progress">
          {STEPS.map((step, index) => {
            const done = index <= reached;
            return (
              <li key={step.status} className={`flex items-center gap-2 rounded-lg border px-3 py-2 text-sm ${done ? 'border-primary/40 bg-primary-light text-primary-dark' : 'border-line text-muted'}`}>
                <span className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full ${done ? 'bg-primary text-white' : 'bg-surface'}`}>
                  {done ? <Check className="h-3 w-3" aria-hidden /> : <span className="text-[10px]">{index + 1}</span>}
                </span>
                {step.label}
              </li>
            );
          })}
        </ol>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card title="Request">
          <DetailList
            items={[
              { label: 'Requested by', value: `${r.requestedByName}, ${formatDateTime(r.createdAt)}` },
              { label: 'Urgency', value: <StatusBadge status={r.urgency} /> },
              { label: 'Needed by', value: formatDate(r.requiredBy) },
              { label: 'Estimated cost', value: formatMoney(r.estimatedCost, currency) },
              ...(r.notes ? [{ label: 'Notes', value: r.notes }] : []),
            ]}
          />
        </Card>
        <Card title="Progress">
          <DetailList
            items={[
              { label: 'Decision', value: r.decidedAt ? `${r.approvedByName}, ${formatDateTime(r.decidedAt)}` : 'Waiting for the project manager' },
              ...(r.remarks && r.status !== 'REJECTED' ? [{ label: 'PM remarks', value: r.remarks }] : []),
              {
                label: 'Purchase order',
                value: r.purchaseOrderId ? (
                  <Link to={`../../purchase-orders/${r.purchaseOrderId}`} relative="path" className="text-primary hover:text-primary-dark">
                    {r.purchaseOrderNumber} ({r.purchaseOrderStatus?.toLowerCase().replace('_', ' ')})
                  </Link>
                ) : '-',
              },
              ...(r.deliveredAt ? [{ label: 'Delivered', value: `${r.deliveredQuantity} ${r.unit}, ${r.deliveredByName}, ${formatDateTime(r.deliveredAt)}` }] : []),
              ...(r.deliveryNotes ? [{ label: 'Delivery notes', value: r.deliveryNotes }] : []),
            ]}
          />
        </Card>
      </div>

      <Modal
        open={dialog !== null}
        title={{ approve: 'Approve request', reject: 'Reject request', order: 'Raise purchase order', deliver: 'Confirm delivery' }[dialog ?? 'approve']}
        onClose={() => setDialog(null)}
        busy={action.isPending}
      >
        <div className="space-y-4">
          {dialog === 'approve' && (
            budget.isLoading ? <Spinner /> : budget.data && (
              <div className={`rounded-lg border p-4 text-sm ${budget.data.overrun ? 'border-red-200 bg-red-50 text-red-800' : 'border-line bg-surface text-ink'}`}>
                <p className="font-medium">{budget.data.phaseName} budget</p>
                <p className="mt-1">
                  Budget {formatMoney(budget.data.phaseBudget, currency)}, already committed {formatMoney(budget.data.committed, currency)},
                  left {formatMoney(budget.data.remaining, currency)}. This request: {formatMoney(budget.data.cost, currency)}.
                </p>
                {budget.data.overrun && (
                  <label className="mt-3 flex items-start gap-2">
                    <input type="checkbox" className="mt-0.5 accent-red-600" checked={acknowledge} onChange={(e) => setAcknowledge(e.target.checked)} />
                    <span className="flex items-center gap-1">
                      <AlertTriangle className="h-4 w-4" aria-hidden />
                      I understand this exceeds the phase budget and still want to approve it.
                    </span>
                  </label>
                )}
              </div>
            )
          )}
          {dialog === 'order' && (
            <>
              <FormField label="Supplier" htmlFor="po-supplier" required>
                <Select
                  id="po-supplier"
                  options={(suppliers.data?.content ?? []).map((s) => ({ value: String(s.id), label: `${s.name} (${s.materialCategories.join(', ')})` }))}
                  placeholder="Choose a supplier"
                  value={supplierId}
                  onChange={(e) => setSupplierId(e.target.value)}
                />
              </FormField>
              <FormField label={`Agreed unit price (${currency})`} htmlFor="po-price" hint={`Total for ${r.quantity} ${r.unit}: ${formatMoney(Number(unitPrice || 0) * Number(r.quantity), currency)}`} required>
                <Input id="po-price" inputMode="decimal" value={unitPrice} onChange={(e) => setUnitPrice(e.target.value)} />
              </FormField>
            </>
          )}
          {dialog === 'deliver' && (
            <FormField label={`Quantity received (${r.unit})`} htmlFor="delivered" hint={`Ordered: ${r.quantity} ${r.unit}`} required>
              <Input id="delivered" inputMode="decimal" value={quantity} onChange={(e) => setQuantity(e.target.value)} />
            </FormField>
          )}
          <FormField
            label={dialog === 'reject' ? 'Reason' : dialog === 'order' ? 'Notes for the supplier (optional)' : dialog === 'deliver' ? 'Delivery notes (optional)' : 'Remarks (optional)'}
            htmlFor="action-remarks"
            required={dialog === 'reject'}
          >
            <Textarea id="action-remarks" rows={3} maxLength={500} value={remarks} onChange={(e) => setRemarks(e.target.value)} />
          </FormField>
          {error && <p className="text-sm text-red-600" role="alert">{error}</p>}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDialog(null)} disabled={action.isPending}>Cancel</Button>
            <Button variant={dialog === 'reject' ? 'danger' : 'primary'} loading={action.isPending} onClick={submit}>
              {{ approve: 'Approve', reject: 'Reject request', order: 'Send purchase order', deliver: 'Confirm delivery' }[dialog ?? 'approve']}
            </Button>
          </div>
        </div>
      </Modal>
    </>
  );
}
