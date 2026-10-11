import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus } from 'lucide-react';
import { attendanceApi } from '../../api/workforceApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { collectErrorMessages, toApiError } from '../../lib/apiError';
import { shiftSchema, type ShiftFormValues } from '../../validation/workforceSchemas';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { Modal } from '../../components/ui/Modal';
import { PageHeader } from '../../components/ui/PageHeader';
import { Spinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { Shift } from '../../types/workforce';

const FIELDS = ['name', 'startTime', 'endTime', 'description', 'active'] as const;

function ShiftForm({ shift, onDone }: { shift: Shift | null; onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } = useForm<ShiftFormValues>({
    resolver: zodResolver(shiftSchema),
    mode: 'onBlur',
    defaultValues: {
      name: shift?.name ?? '', startTime: shift?.startTime.slice(0, 5) ?? '08:00', endTime: shift?.endTime.slice(0, 5) ?? '17:00',
      description: shift?.description ?? '', active: shift?.active ?? true,
    },
  });
  const feedback = useFormFeedback(setError, FIELDS);
  const onSubmit = async (v: ShiftFormValues) => {
    feedback.clear();
    try {
      await attendanceApi.saveShift(shift?.id ?? null, { ...v, description: v.description.trim() || null });
      await queryClient.invalidateQueries({ queryKey: ['shifts'] });
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
      <FormField label="Name" htmlFor="shift-name" error={errors.name?.message} required>
        <Input id="shift-name" invalid={!!errors.name} {...register('name')} />
      </FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Starts" htmlFor="shift-start" error={errors.startTime?.message} required>
          <Input id="shift-start" type="time" invalid={!!errors.startTime} {...register('startTime')} />
        </FormField>
        <FormField label="Ends" htmlFor="shift-end" error={errors.endTime?.message} required>
          <Input id="shift-end" type="time" invalid={!!errors.endTime} {...register('endTime')} />
        </FormField>
      </div>
      <FormField label="Description" htmlFor="shift-desc" error={errors.description?.message}>
        <Input id="shift-desc" maxLength={255} {...register('description')} />
      </FormField>
      <label className="flex items-center gap-2 text-sm text-ink">
        <input type="checkbox" className="accent-primary" {...register('active')} />
        Active
      </label>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={isSubmitting}>Save shift</Button>
      </div>
    </form>
  );
}

/** HR maintains working shifts used when marking attendance. */
export default function ShiftsPage() {
  const { hasPermission } = useAuth();
  const canEdit = hasPermission('EMPLOYEE:EDIT');
  const [form, setForm] = useState<{ shift: Shift | null } | null>(null);
  const shifts = useQuery({ queryKey: ['shifts'], queryFn: attendanceApi.shifts });

  return (
    <>
      <PageHeader title="Shifts" description="Working shifts site engineers can pick when marking attendance."
        actions={canEdit && <Button onClick={() => setForm({ shift: null })}><Plus className="h-4 w-4" aria-hidden />New shift</Button>} />
      {shifts.isLoading ? <Spinner /> : shifts.isError ? <Alert tone="error">{toApiError(shifts.error).message}</Alert> : (
        <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {shifts.data?.map((s) => (
            <li key={s.id}>
              <Card title={s.name} actions={canEdit && (
                <button type="button" onClick={() => setForm({ shift: s })} className="rounded-md p-2 text-muted hover:bg-surface" aria-label={`Edit ${s.name}`}>
                  <Pencil className="h-4 w-4" aria-hidden />
                </button>
              )}>
                <p className="text-2xl font-semibold text-ink">{s.startTime.slice(0, 5)} - {s.endTime.slice(0, 5)}</p>
                {s.description && <p className="mt-1 text-sm text-muted">{s.description}</p>}
                <p className="mt-2"><StatusBadge status={s.active ? 'ACTIVE' : 'INACTIVE'} /></p>
              </Card>
            </li>
          ))}
        </ul>
      )}
      <Modal open={form !== null} title={form?.shift ? 'Edit shift' : 'New shift'} onClose={() => setForm(null)}>
        {form && <ShiftForm shift={form.shift} onDone={() => setForm(null)} />}
      </Modal>
    </>
  );
}
