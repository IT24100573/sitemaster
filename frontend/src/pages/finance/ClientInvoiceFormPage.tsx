import { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useFieldArray, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Plus, Trash2 } from 'lucide-react';
import { clientInvoicesApi } from '../../api/financeApi';
import { useToast } from '../../context/ToastContext';
import { useCurrency } from '../../hooks/useCompanyProfile';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { formatMoney } from '../../lib/format';
import { emptyToNull } from '../../lib/params';
import { todayIso } from '../../validation/common';
import { invoiceSchema, type InvoiceFormValues } from '../../validation/financeSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { Textarea } from '../../components/ui/Textarea';

const FIELDS = ['projectId', 'issueDate', 'dueDate', 'taxRate', 'notes', 'lines'] as const;

function plusDays(days: number) {
  const d = new Date(`${todayIso()}T00:00:00`);
  d.setDate(d.getDate() + days);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/** Create or edit a draft client invoice. Totals are previewed here and recalculated by the server. */
export default function ClientInvoiceFormPage() {
  const { id } = useParams();
  const invoiceId = id ? Number(id) : null;
  const navigate = useNavigate();
  const toast = useToast();
  const currency = useCurrency();
  const queryClient = useQueryClient();
  const existing = useQuery({ queryKey: ['client-invoices', 'detail', invoiceId], queryFn: () => clientInvoicesApi.get(invoiceId as number), enabled: invoiceId !== null });

  const { register, control, handleSubmit, setError, reset, watch, formState: { errors, isSubmitting, submitCount } } = useForm<InvoiceFormValues>({
    resolver: zodResolver(invoiceSchema),
    mode: 'onBlur',
    defaultValues: { projectId: '', issueDate: todayIso(), dueDate: plusDays(30), taxRate: '0', notes: '', lines: [{ description: '', quantity: '1', unitPrice: '' }] },
  });
  const lines = useFieldArray({ control, name: 'lines' });
  const feedback = useFormFeedback(setError, FIELDS);

  useEffect(() => {
    if (existing.data) {
      const i = existing.data;
      reset({
        projectId: String(i.projectId), issueDate: i.issueDate, dueDate: i.dueDate, taxRate: String(Number(i.taxRate)), notes: i.notes ?? '',
        lines: i.lines.map((l) => ({ description: l.description, quantity: String(Number(l.quantity)), unitPrice: Number(l.unitPrice).toFixed(2) })),
      });
    }
  }, [existing.data, reset]);

  const values = watch();
  const subtotal = values.lines.reduce((sum, l) => sum + (Number(l.quantity) || 0) * (Number(l.unitPrice) || 0), 0);
  const tax = (subtotal * (Number(values.taxRate) || 0)) / 100;

  const onSubmit = async (v: InvoiceFormValues) => {
    feedback.clear();
    const payload = { projectId: Number(v.projectId), issueDate: v.issueDate, dueDate: v.dueDate, taxRate: v.taxRate, notes: emptyToNull(v.notes), lines: v.lines };
    try {
      const saved = invoiceId ? await clientInvoicesApi.update(invoiceId, payload) : await clientInvoicesApi.create(payload);
      await queryClient.invalidateQueries({ queryKey: ['client-invoices'] });
      toast.success(`Draft ${saved.invoiceNumber} saved.`);
      navigate(invoiceId ? '..' : `../${saved.id}`, { relative: 'path', replace: true });
    } catch (error) {
      feedback.handleError(error);
    }
  };

  if (invoiceId && existing.isLoading) {
    return <FullPageSpinner />;
  }
  if (invoiceId && (existing.isError || (existing.data && existing.data.status !== 'DRAFT'))) {
    return <Alert tone="error">{existing.isError ? toApiError(existing.error).message : 'Only draft invoices can be edited.'}</Alert>;
  }
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />{invoiceId ? 'Back to invoice' : 'Back to invoices'}
      </Link>
      <PageHeader title={invoiceId ? `Edit ${existing.data?.invoiceNumber}` : 'New client invoice'} description="Saved as a draft. Issue it from the invoice page when it is ready." />
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="max-w-4xl space-y-6">
        <ErrorSummary messages={summary} />
        <Card>
          <div className="grid gap-5 md:grid-cols-2">
            <FormField label="Project" htmlFor="projectId" error={errors.projectId?.message} hint="The client comes from the project." required>
              {invoiceId ? (
                <>
                  <input type="hidden" {...register('projectId')} />
                  <Input id="projectId" value={existing.data?.projectName ?? ''} disabled readOnly />
                </>
              ) : (
                <ProjectSelect id="projectId" placeholder="Choose a project" invalid={!!errors.projectId} {...register('projectId')} />
              )}
            </FormField>
            <FormField label="Tax rate (%)" htmlFor="taxRate" error={errors.taxRate?.message} required>
              <Input id="taxRate" inputMode="decimal" invalid={!!errors.taxRate} {...register('taxRate')} />
            </FormField>
            <FormField label="Invoice date" htmlFor="issueDate" error={errors.issueDate?.message} required>
              <Input id="issueDate" type="date" invalid={!!errors.issueDate} {...register('issueDate')} />
            </FormField>
            <FormField label="Due date" htmlFor="dueDate" error={errors.dueDate?.message} required>
              <Input id="dueDate" type="date" invalid={!!errors.dueDate} {...register('dueDate')} />
            </FormField>
          </div>
        </Card>

        <Card title="Lines">
          <div className="space-y-3">
            {lines.fields.map((line, index) => (
              <div key={line.id} className="grid gap-3 sm:grid-cols-[3fr_1fr_1.4fr_auto] sm:items-start">
                <FormField label="Description" htmlFor={`l-desc-${index}`} error={errors.lines?.[index]?.description?.message}>
                  <Input id={`l-desc-${index}`} {...register(`lines.${index}.description`)} />
                </FormField>
                <FormField label="Qty" htmlFor={`l-qty-${index}`} error={errors.lines?.[index]?.quantity?.message}>
                  <Input id={`l-qty-${index}`} inputMode="decimal" {...register(`lines.${index}.quantity`)} />
                </FormField>
                <FormField label={`Unit price (${currency})`} htmlFor={`l-price-${index}`} error={errors.lines?.[index]?.unitPrice?.message}>
                  <Input id={`l-price-${index}`} inputMode="decimal" {...register(`lines.${index}.unitPrice`)} />
                </FormField>
                <button type="button" onClick={() => lines.remove(index)} disabled={lines.fields.length === 1}
                  className="mt-7 rounded-md p-2 text-red-600 hover:bg-surface disabled:opacity-30" aria-label="Remove line">
                  <Trash2 className="h-4 w-4" aria-hidden />
                </button>
              </div>
            ))}
            {errors.lines?.message && <p className="text-sm text-red-600">{errors.lines.message}</p>}
            <Button type="button" variant="secondary" size="sm" disabled={lines.fields.length >= 50} onClick={() => lines.append({ description: '', quantity: '1', unitPrice: '' })}>
              <Plus className="h-4 w-4" aria-hidden />Add line
            </Button>
          </div>
          <dl className="mt-6 ml-auto max-w-xs space-y-1 text-sm">
            <div className="flex justify-between"><dt className="text-muted">Subtotal</dt><dd>{formatMoney(subtotal, currency)}</dd></div>
            <div className="flex justify-between"><dt className="text-muted">Tax</dt><dd>{formatMoney(tax, currency)}</dd></div>
            <div className="flex justify-between font-semibold text-ink"><dt>Total</dt><dd>{formatMoney(subtotal + tax, currency)}</dd></div>
          </dl>
        </Card>

        <FormField label="Notes" htmlFor="notes" error={errors.notes?.message} hint="Printed on the invoice, e.g. bank details.">
          <Textarea id="notes" rows={3} maxLength={500} {...register('notes')} />
        </FormField>
        <div className="flex gap-2">
          <Button type="submit" loading={isSubmitting}>Save draft</Button>
          <Button type="button" variant="secondary" onClick={() => navigate('..', { relative: 'path' })}>Cancel</Button>
        </div>
      </form>
    </>
  );
}
