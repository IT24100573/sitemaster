import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { attendanceApi } from '../../api/workforceApi';
import { useToast } from '../../context/ToastContext';
import { useFormFeedback } from '../../hooks/useFormFeedback';
import { useEnumLookup } from '../../hooks/useLookup';
import { collectErrorMessages } from '../../lib/apiError';
import { emptyToNull } from '../../lib/params';
import { todayIso } from '../../validation/common';
import { attendanceSchema, type AttendanceFormValues } from '../../validation/workforceSchemas';
import { Button } from '../../components/ui/Button';
import { ErrorSummary } from '../../components/ui/ErrorSummary';
import { FormField } from '../../components/ui/FormField';
import { Input } from '../../components/ui/Input';
import { ProjectSelect } from '../../components/ui/ProjectSelect';
import { Select } from '../../components/ui/Select';
import { Textarea } from '../../components/ui/Textarea';
import type { AttendanceRecord } from '../../types/workforce';

const FIELDS = ['projectId', 'employeeId', 'date', 'status', 'shiftId', 'checkIn', 'checkOut', 'overtimeHours', 'overtimeReason', 'notes'] as const;

function hhmm(value: string | null): string {
  return value ? value.slice(11, 16) : '';
}

/** Mark a day for a team member, or correct an existing record. Saving also verifies it. */
export function AttendanceForm({ record, onDone }: { record: AttendanceRecord | null; onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const statuses = useEnumLookup('attendance-statuses');
  const shifts = useQuery({ queryKey: ['shifts'], queryFn: attendanceApi.shifts });
  const { register, handleSubmit, setError, watch, formState: { errors, isSubmitting, submitCount } } = useForm<AttendanceFormValues>({
    resolver: zodResolver(attendanceSchema),
    mode: 'onBlur',
    defaultValues: record
      ? {
          projectId: String(record.projectId), employeeId: String(record.employeeId), date: record.date, status: record.status,
          shiftId: record.shiftId ? String(record.shiftId) : '', checkIn: hhmm(record.checkIn), checkOut: hhmm(record.checkOut),
          overtimeHours: record.overtimeHours ? String(record.overtimeHours) : '', overtimeReason: '', notes: record.notes ?? '',
        }
      : { projectId: '', employeeId: '', date: todayIso(), status: 'PRESENT', shiftId: '', checkIn: '08:00', checkOut: '17:00', overtimeHours: '', overtimeReason: '', notes: '' },
  });
  const feedback = useFormFeedback(setError, FIELDS);
  const projectId = watch('projectId');
  const status = watch('status');
  const working = status === 'PRESENT' || status === 'HALF_DAY';
  const team = useQuery({
    queryKey: ['attendance', 'team', projectId],
    queryFn: () => attendanceApi.team(Number(projectId)),
    enabled: projectId !== '' && !record,
  });
  const overtimeDecided = record?.overtimeStatus && record.overtimeStatus !== 'PENDING';

  const onSubmit = async (v: AttendanceFormValues) => {
    feedback.clear();
    const payload = {
      employeeId: Number(v.employeeId),
      projectId: Number(v.projectId),
      date: v.date,
      status: v.status,
      shiftId: v.shiftId ? Number(v.shiftId) : null,
      checkIn: working ? emptyToNull(v.checkIn) : null,
      checkOut: working ? emptyToNull(v.checkOut) : null,
      overtimeHours: working && !overtimeDecided ? emptyToNull(v.overtimeHours) : overtimeDecided ? String(record?.overtimeHours) : null,
      overtimeReason: emptyToNull(v.overtimeReason),
      notes: emptyToNull(v.notes),
    };
    try {
      if (record) {
        await attendanceApi.correct(record.id, payload);
      } else {
        await attendanceApi.mark(payload);
      }
      await queryClient.invalidateQueries({ queryKey: ['attendance'] });
      toast.success(record ? 'Attendance corrected and verified.' : 'Attendance recorded and verified.');
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
        <FormField label="Project" htmlFor="att-project" error={errors.projectId?.message} required>
          <ProjectSelect id="att-project" activeOnly placeholder="Choose a project" invalid={!!errors.projectId} {...register('projectId')} />
        </FormField>
        <FormField label="Employee" htmlFor="att-employee" error={errors.employeeId?.message} required>
          {record ? (
            <>
              <input type="hidden" {...register('employeeId')} />
              <Input id="att-employee" value={`${record.employeeName} (${record.employeeNo})`} disabled readOnly />
            </>
          ) : (
            <Select id="att-employee" options={team.data ?? []} placeholder={projectId ? 'Choose a team member' : 'Choose a project first'}
              invalid={!!errors.employeeId} {...register('employeeId')} />
          )}
        </FormField>
        <FormField label="Date" htmlFor="att-date" error={errors.date?.message} required>
          <Input id="att-date" type="date" max={todayIso()} readOnly={!!record} invalid={!!errors.date} {...register('date')} />
        </FormField>
        <FormField label="Status" htmlFor="att-status" error={errors.status?.message} required>
          <Select id="att-status" options={statuses.data ?? []} invalid={!!errors.status} {...register('status')} />
        </FormField>
        {working && (
          <>
            <FormField label="Check-in" htmlFor="att-in" error={errors.checkIn?.message}>
              <Input id="att-in" type="time" invalid={!!errors.checkIn} {...register('checkIn')} />
            </FormField>
            <FormField label="Check-out" htmlFor="att-out" error={errors.checkOut?.message}>
              <Input id="att-out" type="time" invalid={!!errors.checkOut} {...register('checkOut')} />
            </FormField>
            <FormField label="Shift" htmlFor="att-shift" error={errors.shiftId?.message}>
              <Select id="att-shift" options={(shifts.data ?? []).filter((s) => s.active).map((s) => ({ value: String(s.id), label: `${s.name} ${s.startTime.slice(0, 5)}-${s.endTime.slice(0, 5)}` }))}
                placeholder="No shift" {...register('shiftId')} />
            </FormField>
            <FormField label="Overtime hours" htmlFor="att-ot" error={errors.overtimeHours?.message}
              hint={overtimeDecided ? `Already ${record?.overtimeStatus?.toLowerCase()} by HR` : '0-8; HR approves before payroll'}>
              <Input id="att-ot" inputMode="decimal" disabled={!!overtimeDecided} invalid={!!errors.overtimeHours} {...register('overtimeHours')} />
            </FormField>
            <div className="sm:col-span-2">
              <FormField label="Overtime reason" htmlFor="att-ot-reason" error={errors.overtimeReason?.message}>
                <Input id="att-ot-reason" maxLength={500} disabled={!!overtimeDecided} {...register('overtimeReason')} />
              </FormField>
            </div>
          </>
        )}
        <div className="sm:col-span-2">
          <FormField label="Notes" htmlFor="att-notes" error={errors.notes?.message}>
            <Textarea id="att-notes" rows={2} maxLength={500} {...register('notes')} />
          </FormField>
        </div>
      </div>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>Cancel</Button>
        <Button type="submit" loading={isSubmitting}>{record ? 'Save correction' : 'Save attendance'}</Button>
      </div>
    </form>
  );
}
