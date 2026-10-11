import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { leaveApi } from '../../api/workforceApi';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useTableState } from '../../hooks/useTableState';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { formatDate } from '../../lib/format';
import { todayIso } from '../../validation/common';
import { leaveSchema, type LeaveFormValues } from '../../validation/workforceSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { Select } from '../../components/ui/Select';
import { Spinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Textarea } from '../../components/ui/Textarea';
import type { LeaveRequestRecord } from '../../types/workforce';

const FIELDS = ['leaveTypeId', 'fromDate', 'toDate', 'reason'] as const;

function ApplyForm({ onDone }: { onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const types = useQuery({ queryKey: ['leave-types', 'active'], queryFn: () => leaveApi.types(true) });
  const { register, handleSubmit, setError, watch, formState: { errors, isSubmitting, submitCount } } = useForm<LeaveFormValues>({
    resolver: zodResolver(leaveSchema),
    mode: 'onBlur',
    defaultValues: { leaveTypeId: '', fromDate: todayIso(), toDate: todayIso(), reason: '' },
  });
  const feedback = useFormFeedback(setError, FIELDS);
  const selected = types.data?.find((t) => String(t.id) === watch('leaveTypeId'));

  const onSubmit = async (v: LeaveFormValues) => {
    feedback.clear();
    try {
      const saved = await leaveApi.apply({ leaveTypeId: Number(v.leaveTypeId), fromDate: v.fromDate, toDate: v.toDate, reason: v.reason });
      await queryClient.invalidateQueries({ queryKey: ['leave'] });
      toast.success(`Applied for ${saved.days} day(s). HR has been notified.`);
      onDone();
    } catch (error) {
      feedback.handleError(error);
    }
  };
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <FormField label="Leave type" htmlFor="leaveTypeId" error={errors.leaveTypeId?.message}
        hint={selected && selected.backdateDays > 0 ? `Can be applied up to ${selected.backdateDays} day(s) after it started.` : undefined} required>
        <Select id="leaveTypeId" options={(types.data ?? []).map((t) => ({ value: String(t.id), label: `${t.name}${t.paid ? '' : ' (unpaid)'}` }))}
          placeholder="Choose a leave type" invalid={!!errors.leaveTypeId} {...register('leaveTypeId')} />
      </FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="From" htmlFor="fromDate" error={errors.fromDate?.message} required>
          <Input id="fromDate" type="date" invalid={!!errors.fromDate} {...register('fromDate')} />
        </FormField>
        <FormField label="To" htmlFor="toDate" error={errors.toDate?.message} required>
          <Input id="toDate" type="date" invalid={!!errors.toDate} {...register('toDate')} />
        </FormField>
      </div>
      <p className="text-xs text-muted">Sundays and public holidays are not counted.</p>
      <FormField label="Reason" htmlFor="reason" error={errors.reason?.message} required>
        <Textarea id="reason" rows={3} maxLength={500} invalid={!!errors.reason} {...register('reason')} />
      </FormField>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={isSubmitting}>Apply</Button>
      </div>
    </form>
  );
}

/** Any employee: leave balances, apply, follow and cancel requests. */
export default function MyLeavePage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const year = new Date().getFullYear();
  const table = useTableState('fromDate', 'desc');
  const [applying, setApplying] = useState(false);
  const [cancelling, setCancelling] = useState<LeaveRequestRecord | null>(null);

  const balances = useQuery({ queryKey: ['leave', 'balances', year], queryFn: () => leaveApi.myBalances(year) });
  const requests = useQuery({ queryKey: ['leave', 'mine', table.params], queryFn: () => leaveApi.mine(table.params), placeholderData: keepPreviousData });

  const cancel = useMutation({
    mutationFn: (id: number) => leaveApi.cancel(id),
    onSuccess: async () => {
      setCancelling(null);
      await queryClient.invalidateQueries({ queryKey: ['leave'] });
      toast.success('Leave request cancelled.');
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const columns: Column<LeaveRequestRecord>[] = [
    { key: 'type', header: 'Leave', render: (r) => r.leaveTypeName },
    { key: 'dates', header: 'Dates', sortKey: 'fromDate', className: 'whitespace-nowrap', render: (r) => `${formatDate(r.fromDate)} to ${formatDate(r.toDate)}` },
    { key: 'days', header: 'Days', render: (r) => r.days },
    { key: 'status', header: 'Status', sortKey: 'status', render: (r) => <StatusBadge status={r.status} /> },
    { key: 'remarks', header: 'HR remarks', render: (r) => <span className="text-muted">{r.remarks ?? '-'}</span> },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (r) => r.canCancel && <Button size="sm" variant="secondary" onClick={() => setCancelling(r)}>Cancel</Button>,
    },
  ];

  return (
    <>
      <PageHeader
        title="My leave"
        description={`Your ${year} leave balances and requests.`}
        actions={<Button onClick={() => setApplying(true)}><Plus className="h-4 w-4" aria-hidden />Apply for leave</Button>}
      />
      {balances.isLoading ? <Spinner /> : balances.isError ? (
        <div className="mb-6"><Alert tone="error">{toApiError(balances.error).message}</Alert></div>
      ) : (
        <ul className="mb-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {balances.data?.map((b) => (
            <li key={b.leaveTypeId} className="card p-4">
              <p className="text-sm text-muted">{b.leaveTypeName}</p>
              <p className="mt-1 text-2xl font-semibold text-ink">{b.available}<span className="text-sm font-normal text-muted"> / {b.entitled} days</span></p>
              <p className="mt-1 text-xs text-muted">Used {b.used}{Number(b.pending) > 0 ? `, ${b.pending} pending` : ''}{b.paid ? '' : ', unpaid'}</p>
            </li>
          ))}
        </ul>
      )}
      <DataTable
        columns={columns}
        page={requests.data}
        rowKey={(r) => r.id}
        loading={requests.isFetching}
        error={requests.isError ? toApiError(requests.error).message : null}
        emptyMessage="You have not applied for leave yet."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
      />
      <Modal open={applying} title="Apply for leave" onClose={() => setApplying(false)}>
        {applying && <ApplyForm onDone={() => setApplying(false)} />}
      </Modal>
      <ConfirmDialog
        open={cancelling !== null}
        title="Cancel this leave request?"
        message={cancelling ? `${cancelling.leaveTypeName}, ${formatDate(cancelling.fromDate)} to ${formatDate(cancelling.toDate)}.` : ''}
        confirmLabel="Cancel request"
        tone="danger"
        loading={cancel.isPending}
        onConfirm={() => cancelling && cancel.mutate(cancelling.id)}
        onCancel={() => setCancelling(null)}
      />
    </>
  );
}
