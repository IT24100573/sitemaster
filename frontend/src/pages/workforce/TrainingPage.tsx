import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, HardHat, Pencil, Plus, Trash2 } from 'lucide-react';
import { hrRecordsApi } from '../../api/workforceApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useTableState } from '../../hooks/useTableState';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { formatDate } from '../../lib/format';
import { emptyToNull } from '../../lib/params';
import { todayIso } from '../../validation/common';
import { trainingSchema, type TrainingFormValues } from '../../validation/workforceSchemas';
import { Button } from '../../components/ui/Button';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { DataTable, type Column } from '../../components/ui/DataTable';
import { EmployeeSelect } from '../../components/ui/EmployeeSelect';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FileUpload } from '../../components/ui/FileUpload';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { SearchInput } from '../../components/ui/SearchInput';
import { Select } from '../../components/ui/Select';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Textarea } from '../../components/ui/Textarea';
import type { TrainingRecord } from '../../types/workforce';

const FIELDS = ['employeeId', 'title', 'provider', 'safety', 'completedOn', 'expiresOn', 'notes'] as const;

function TrainingForm({ record, onDone }: { record: TrainingRecord | null; onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [file, setFile] = useState<File | null>(null);
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } = useForm<TrainingFormValues>({
    resolver: zodResolver(trainingSchema),
    mode: 'onBlur',
    defaultValues: {
      employeeId: record ? String(record.employeeId) : '', title: record?.title ?? '', provider: record?.provider ?? '',
      safety: record?.safety ?? false, completedOn: record?.completedOn ?? todayIso(), expiresOn: record?.expiresOn ?? '', notes: record?.notes ?? '',
    },
  });
  const feedback = useFormFeedback(setError, FIELDS);
  const onSubmit = async (v: TrainingFormValues) => {
    feedback.clear();
    try {
      const saved = await hrRecordsApi.saveTraining(record?.id ?? null, {
        employeeId: Number(v.employeeId), title: v.title, provider: emptyToNull(v.provider), safety: v.safety,
        completedOn: v.completedOn, expiresOn: emptyToNull(v.expiresOn), notes: emptyToNull(v.notes),
      });
      if (file) {
        try {
          await hrRecordsApi.uploadCertificate(saved.id, file);
        } catch (error) {
          toast.error(`Record saved, but the certificate was not uploaded: ${toApiError(error).message}`);
        }
      }
      await queryClient.invalidateQueries({ queryKey: ['training'] });
      toast.success(`${saved.title} saved.`);
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
        <FormField label="Employee" htmlFor="t-employee" error={errors.employeeId?.message} required>
          <EmployeeSelect id="t-employee" placeholder="Choose an employee" invalid={!!errors.employeeId} {...register('employeeId')} />
        </FormField>
        <FormField label="Course or certificate" htmlFor="t-title" error={errors.title?.message} required>
          <Input id="t-title" invalid={!!errors.title} {...register('title')} />
        </FormField>
        <FormField label="Provider" htmlFor="t-provider" error={errors.provider?.message}>
          <Input id="t-provider" {...register('provider')} />
        </FormField>
        <label className="flex items-center gap-2 self-end pb-3 text-sm text-ink">
          <input type="checkbox" className="accent-primary" {...register('safety')} />
          Safety training
        </label>
        <FormField label="Completed on" htmlFor="t-completed" error={errors.completedOn?.message} required>
          <Input id="t-completed" type="date" max={todayIso()} invalid={!!errors.completedOn} {...register('completedOn')} />
        </FormField>
        <FormField label="Expires on" htmlFor="t-expires" error={errors.expiresOn?.message}>
          <Input id="t-expires" type="date" invalid={!!errors.expiresOn} {...register('expiresOn')} />
        </FormField>
      </div>
      <FormField label="Notes" htmlFor="t-notes" error={errors.notes?.message}>
        <Textarea id="t-notes" rows={2} maxLength={500} {...register('notes')} />
      </FormField>
      <FileUpload kind="document" file={file} onChange={setFile} label={record?.fileName ? `Replace ${record.fileName}` : 'Attach the certificate (optional)'} />
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={isSubmitting}>Save</Button>
      </div>
    </form>
  );
}

/** HR: training courses and safety certifications per employee. */
export default function TrainingPage() {
  const { hasPermission } = useAuth();
  const canEdit = hasPermission('EMPLOYEE:EDIT');
  const canDelete = hasPermission('EMPLOYEE:DELETE');
  const toast = useToast();
  const queryClient = useQueryClient();
  const table = useTableState('completedOn', 'desc');
  const [employeeId, setEmployeeId] = useState('');
  const [safety, setSafety] = useState('');
  const [form, setForm] = useState<{ record: TrainingRecord | null } | null>(null);
  const [deleting, setDeleting] = useState<TrainingRecord | null>(null);

  const records = useQuery({
    queryKey: ['training', table.params, employeeId, safety],
    queryFn: () => hrRecordsApi.training(table.params, { employeeId: employeeId || undefined, safety: safety || undefined }),
    placeholderData: keepPreviousData,
  });
  const remove = useMutation({
    mutationFn: (id: number) => hrRecordsApi.deleteTraining(id),
    onSuccess: async () => {
      setDeleting(null);
      await queryClient.invalidateQueries({ queryKey: ['training'] });
      toast.success('Training record deleted.');
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const columns: Column<TrainingRecord>[] = [
    { key: 'title', header: 'Training', sortKey: 'title', render: (t) => (
      <div className="flex items-start gap-2">
        {t.safety && <HardHat className="mt-0.5 h-4 w-4 shrink-0 text-primary" aria-label="Safety training" />}
        <div><p className="font-medium text-ink">{t.title}</p>{t.provider && <p className="text-xs text-muted">{t.provider}</p>}</div>
      </div>
    ) },
    { key: 'employee', header: 'Employee', render: (t) => `${t.employeeName} (${t.employeeNo})` },
    { key: 'completed', header: 'Completed', sortKey: 'completedOn', className: 'whitespace-nowrap', render: (t) => formatDate(t.completedOn) },
    { key: 'expires', header: 'Expires', sortKey: 'expiresOn', className: 'whitespace-nowrap', render: (t) => (t.expiresOn ? <span className="flex items-center gap-1">{formatDate(t.expiresOn)}{t.expired && <StatusBadge status="EXPIRED" />}</span> : 'No expiry') },
    {
      key: 'actions',
      header: 'Actions',
      className: 'text-right',
      render: (t) => (
        <div className="flex justify-end gap-1">
          {t.fileName && <a href={hrRecordsApi.certificateUrl(t.id)} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Download certificate for ${t.title}`}><Download className="h-4 w-4" aria-hidden /></a>}
          {canEdit && <button type="button" onClick={() => setForm({ record: t })} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Edit ${t.title}`}><Pencil className="h-4 w-4" aria-hidden /></button>}
          {canDelete && <button type="button" onClick={() => setDeleting(t)} className="rounded-md p-2 text-red-600 hover:bg-surface" aria-label={`Delete ${t.title}`}><Trash2 className="h-4 w-4" aria-hidden /></button>}
        </div>
      ),
    },
  ];

  return (
    <>
      <PageHeader title="Training & safety" description="Courses and certifications, with expired certificates flagged."
        actions={canEdit && <Button onClick={() => setForm({ record: null })}><Plus className="h-4 w-4" aria-hidden />Add record</Button>} />
      <DataTable
        columns={columns}
        page={records.data}
        rowKey={(t) => t.id}
        loading={records.isFetching}
        error={records.isError ? toApiError(records.error).message : null}
        emptyMessage="No training records match these filters."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Course or employee" />
            <div className="flex flex-wrap gap-2 sm:ml-auto">
              <EmployeeSelect aria-label="Filter by employee" value={employeeId} onChange={(e) => { setEmployeeId(e.target.value); table.resetPage(); }} />
              <Select aria-label="Filter by type" options={[{ value: 'true', label: 'Safety only' }, { value: 'false', label: 'Other training' }]}
                placeholder="All training" value={safety} onChange={(e) => { setSafety(e.target.value); table.resetPage(); }} />
            </div>
          </>
        }
      />
      <Modal open={form !== null} size="lg" title={form?.record ? 'Edit training record' : 'Add training record'} onClose={() => setForm(null)}>
        {form && <TrainingForm record={form.record} onDone={() => setForm(null)} />}
      </Modal>
      <ConfirmDialog open={deleting !== null} title="Delete this training record?" message={deleting ? `${deleting.title} for ${deleting.employeeName}.` : ''}
        confirmLabel="Delete" tone="danger" loading={remove.isPending} onConfirm={() => deleting && remove.mutate(deleting.id)} onCancel={() => setDeleting(null)} />
    </>
  );
}
