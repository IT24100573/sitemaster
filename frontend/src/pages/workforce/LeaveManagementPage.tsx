import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus } from 'lucide-react';
import { z } from 'zod';
import { leaveApi } from '../../api/workforceApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useTableState } from '../../hooks/useTableState';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { formatDate, formatDateTime } from '../../lib/format';
import { CLIENT_MESSAGES as MSG } from '../../validation/messages';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { Spinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Tabs } from '../../components/ui/Tabs';
import { Textarea } from '../../components/ui/Textarea';
import type { LeaveRequestRecord, LeaveType } from '../../types/workforce';

const typeSchema = z.object({
  code: z.string().trim().regex(/^[A-Za-z_]{2,20}$/, MSG.leaveCode),
  name: z.string().trim().min(2, MSG.required).max(50),
  daysPerYear: z.string().trim().refine((v) => v === '' || (/^\d{1,3}(\.\d)?$/.test(v) && Number(v) <= 365), 'Enter 0-365 days.'),
  paid: z.boolean(),
  backdateDays: z.string().regex(/^\d{1,2}$/, 'Enter 0-31 days.').refine((v) => Number(v) <= 31, 'Enter 0-31 days.'),
  active: z.boolean(),
});
type TypeValues = z.infer<typeof typeSchema>;
const TYPE_FIELDS = ['code', 'name', 'daysPerYear', 'paid', 'backdateDays', 'active'] as const;

function TypeForm({ type, onDone }: { type: LeaveType | null; onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } = useForm<TypeValues>({
    resolver: zodResolver(typeSchema),
    mode: 'onBlur',
    defaultValues: {
      code: type?.code ?? '', name: type?.name ?? '', daysPerYear: type?.daysPerYear != null ? String(type.daysPerYear) : '',
      paid: type?.paid ?? true, backdateDays: String(type?.backdateDays ?? 0), active: type?.active ?? true,
    },
  });
  const feedback = useFormFeedback(setError, TYPE_FIELDS);
  const onSubmit = async (v: TypeValues) => {
    feedback.clear();
    if (!type?.fromSettings && v.daysPerYear === '') {
      setError('daysPerYear', { type: 'required', message: MSG.required });
      return;
    }
    try {
      await leaveApi.saveType(type?.id ?? null, {
        code: v.code.toUpperCase(), name: v.name, daysPerYear: v.daysPerYear === '' ? null : Number(v.daysPerYear),
        paid: v.paid, backdateDays: Number(v.backdateDays), active: v.active,
      });
      await queryClient.invalidateQueries({ queryKey: ['leave-types'] });
      toast.success(`${v.name} saved.`);
      onDone();
    } catch (error) {
      feedback.handleError(error);
    }
  };
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];
  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Code" htmlFor="lt-code" error={errors.code?.message} required>
          <Input id="lt-code" className="uppercase" readOnly={!!type} invalid={!!errors.code} {...register('code')} />
        </FormField>
        <FormField label="Name" htmlFor="lt-name" error={errors.name?.message} required>
          <Input id="lt-name" invalid={!!errors.name} {...register('name')} />
        </FormField>
        <FormField label="Days per year" htmlFor="lt-days" error={errors.daysPerYear?.message}
          hint={type?.fromSettings ? 'Set in System Settings by the Admin.' : undefined} required={!type?.fromSettings}>
          <Input id="lt-days" inputMode="decimal" disabled={type?.fromSettings} invalid={!!errors.daysPerYear} {...register('daysPerYear')} />
        </FormField>
        <FormField label="Back-dating allowed (days)" htmlFor="lt-back" error={errors.backdateDays?.message}>
          <Input id="lt-back" inputMode="numeric" invalid={!!errors.backdateDays} {...register('backdateDays')} />
        </FormField>
      </div>
      <div className="flex gap-6 text-sm text-ink">
        <label className="flex items-center gap-2"><input type="checkbox" className="accent-primary" {...register('paid')} />Paid leave</label>
        <label className="flex items-center gap-2"><input type="checkbox" className="accent-primary" {...register('active')} />Active</label>
      </div>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={isSubmitting}>Save</Button>
      </div>
    </form>
  );
}

/** HR: leave requests to decide, and leave types. */
export default function LeaveManagementPage() {
  const { hasPermission } = useAuth();
  const canDecide = hasPermission('LEAVE:APPROVE');
  const canEditTypes = hasPermission('LEAVE:EDIT');
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState('requests');
  const table = useTableState('createdAt', 'desc');
  const [status, setStatus] = useState('PENDING');
  const [decision, setDecision] = useState<{ item: LeaveRequestRecord; approve: boolean } | null>(null);
  const [remarks, setRemarks] = useState('');
  const [error, setError] = useState<string | undefined>();
  const [typeForm, setTypeForm] = useState<{ type: LeaveType | null } | null>(null);

  const requests = useQuery({
    queryKey: ['leave', 'all', table.params, status],
    queryFn: () => leaveApi.search(table.params, status || undefined),
    placeholderData: keepPreviousData,
  });
  const types = useQuery({ queryKey: ['leave-types', 'all'], queryFn: () => leaveApi.types(false), enabled: tab === 'types' });

  const decide = useMutation({
    mutationFn: () => decision!.approve ? leaveApi.approve(decision!.item.id, remarks.trim() || null) : leaveApi.reject(decision!.item.id, remarks.trim()),
    onSuccess: async (r) => {
      setDecision(null);
      await queryClient.invalidateQueries({ queryKey: ['leave'] });
      toast.success(`${r.employeeName}'s leave ${r.status.toLowerCase()}.`);
    },
    onError: (err) => {
      const apiError = toApiError(err);
      setError(Object.values(apiError.fieldErrors)[0] ?? apiError.message);
    },
  });

  const columns: Column<LeaveRequestRecord>[] = [
    { key: 'employee', header: 'Employee', render: (r) => `${r.employeeName} (${r.employeeNo})` },
    { key: 'type', header: 'Leave', render: (r) => r.leaveTypeName },
    { key: 'dates', header: 'Dates', sortKey: 'fromDate', className: 'whitespace-nowrap', render: (r) => `${formatDate(r.fromDate)} to ${formatDate(r.toDate)}` },
    { key: 'days', header: 'Days', render: (r) => r.days },
    { key: 'reason', header: 'Reason', render: (r) => <span className="line-clamp-2 text-muted">{r.reason}</span> },
    { key: 'applied', header: 'Applied', sortKey: 'createdAt', className: 'whitespace-nowrap', render: (r) => formatDateTime(r.createdAt) },
    { key: 'status', header: 'Status', sortKey: 'status', render: (r) => <StatusBadge status={r.status} /> },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (r) => canDecide && r.canDecide && (
        <div className="flex justify-end gap-2">
          <Button size="sm" variant="secondary" onClick={() => { setDecision({ item: r, approve: false }); setRemarks(''); setError(undefined); }}>Reject</Button>
          <Button size="sm" onClick={() => { setDecision({ item: r, approve: true }); setRemarks(''); setError(undefined); }}>Approve</Button>
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader title="Leave" description="Approving deducts the days from the employee's balance."
        actions={tab === 'types' && canEditTypes && <Button onClick={() => setTypeForm({ type: null })}><Plus className="h-4 w-4" aria-hidden />New leave type</Button>} />
      <Tabs label="Leave sections" tabs={[{ id: 'requests', label: 'Requests' }, { id: 'types', label: 'Leave types' }]} active={tab} onChange={setTab} />
      <div className="mt-6">
        {tab === 'requests' ? (
          <DataTable
            columns={columns}
            page={requests.data}
            rowKey={(r) => r.id}
            loading={requests.isFetching}
            error={requests.isError ? toApiError(requests.error).message : null}
            emptyMessage="No leave requests match these filters."
            sort={table.sort}
            direction={table.direction}
            onSort={table.toggleSort}
            onPageChange={table.setPage}
            toolbar={
              <>
                <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Employee name or number" />
                <div className="sm:ml-auto">
                  <Select aria-label="Filter by status" options={[{ value: 'PENDING', label: 'Pending' }, { value: 'APPROVED', label: 'Approved' }, { value: 'REJECTED', label: 'Rejected' }]}
                    placeholder="Any status" value={status} onChange={(e) => { setStatus(e.target.value); table.resetPage(); }} />
                </div>
              </>
            }
          />
        ) : types.isLoading ? <Spinner /> : types.isError ? <Alert tone="error">{toApiError(types.error).message}</Alert> : (
          <div className="card overflow-x-auto p-0">
            <table className="w-full min-w-[560px] text-sm">
              <thead className="bg-surface text-left text-muted">
                <tr>
                  <th scope="col" className="px-4 py-3 font-medium">Leave type</th>
                  <th scope="col" className="px-4 py-3 font-medium">Days / year</th>
                  <th scope="col" className="px-4 py-3 font-medium">Paid</th>
                  <th scope="col" className="px-4 py-3 font-medium">Back-dating</th>
                  <th scope="col" className="px-4 py-3 font-medium">Status</th>
                  <th scope="col" className="px-4 py-3 text-right font-medium">Actions</th>
                </tr>
              </thead>
              <tbody>
                {types.data?.map((t) => (
                  <tr key={t.id} className="border-t border-line">
                    <td className="px-4 py-3"><p className="font-medium text-ink">{t.name}</p><p className="text-xs text-muted">{t.code}</p></td>
                    <td className="px-4 py-3">{t.entitlement}{t.fromSettings ? <span className="text-xs text-muted"> (System Settings)</span> : ''}</td>
                    <td className="px-4 py-3">{t.paid ? 'Yes' : 'No'}</td>
                    <td className="px-4 py-3">{t.backdateDays > 0 ? `${t.backdateDays} day(s)` : 'None'}</td>
                    <td className="px-4 py-3"><StatusBadge status={t.active ? 'ACTIVE' : 'INACTIVE'} /></td>
                    <td className="px-4 py-3 text-right">
                      {canEditTypes && (
                        <button type="button" onClick={() => setTypeForm({ type: t })} className="rounded-md p-2 text-muted hover:bg-surface" aria-label={`Edit ${t.name}`}>
                          <Pencil className="h-4 w-4" aria-hidden />
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      <Modal open={decision !== null} title={decision?.approve ? 'Approve leave' : 'Reject leave'}
        description={decision ? `${decision.item.employeeName}: ${decision.item.days} day(s) of ${decision.item.leaveTypeName}, ${formatDate(decision.item.fromDate)} to ${formatDate(decision.item.toDate)}` : undefined}
        onClose={() => setDecision(null)} busy={decide.isPending}>
        <div className="space-y-4">
          <FormField label={decision?.approve ? 'Remarks (optional)' : 'Reason'} htmlFor="leave-remarks" error={error} required={!decision?.approve}>
            <Textarea id="leave-remarks" rows={3} maxLength={500} invalid={!!error} value={remarks} onChange={(e) => setRemarks(e.target.value)} />
          </FormField>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setDecision(null)}>Cancel</Button>
            <Button variant={decision?.approve ? 'primary' : 'danger'} loading={decide.isPending}
              onClick={() => (!decision?.approve && remarks.trim().length < 5 ? setError(MSG.remarksRequired) : decide.mutate())}>
              {decision?.approve ? 'Approve' : 'Reject'}
            </Button>
          </div>
        </div>
      </Modal>
      <Modal open={typeForm !== null} title={typeForm?.type ? 'Edit leave type' : 'New leave type'} onClose={() => setTypeForm(null)}>
        {typeForm && <TypeForm type={typeForm.type} onDone={() => setTypeForm(null)} />}
      </Modal>
    </>
  );
}
