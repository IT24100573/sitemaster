import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Circle, Pencil, PlayCircle, Trash2 } from 'lucide-react';
import { tasksApi } from '../../../api/tasksApi';
import { useToast } from '../../../context/ToastContext';
import { useFormFeedback } from '../../../hooks/useFormFeedback';
import { useEnumLookup } from '../../../hooks/useLookup';
import { collectErrorMessages, toApiError } from '../../../lib/apiError';
import { formatDate, humanize } from '../../../lib/format';
import { emptyToNull } from '../../../lib/params';
import {
  subTaskSchema,
  taskSchema,
  type SubTaskFormValues,
  type TaskFormValues,
} from '../../../validation/projectSchemas';
import { Button } from '../../../components/ui/Button';
import { ErrorSummary } from '../../../components/ui/ErrorSummary';
import { FormField } from '../../../components/ui/FormField';
import { Input } from '../../../components/ui/Input';
import { ProgressBar } from '../../../components/ui/ProgressBar';
import { Select } from '../../../components/ui/Select';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { Textarea } from '../../../components/ui/Textarea';
import { projectKeys } from '../queryKeys';
import type { ProjectPermissions } from '../useProjectPermissions';
import type { LookupOption } from '../../../types/common';
import type { Phase, ProjectDetail } from '../../../types/project';
import type { SubTask, Task, TaskStatus } from '../../../types/task';

const TASK_FIELDS = ['phaseId', 'title', 'description', 'assigneeId', 'priority', 'dueDate'] as const;
const SUB_FIELDS = ['title', 'assigneeId', 'dueDate'] as const;

export async function refreshProjectTasks(queryClient: ReturnType<typeof useQueryClient>, projectId: number) {
  await queryClient.invalidateQueries({ queryKey: projectKeys.tasks(projectId) });
  await queryClient.invalidateQueries({ queryKey: projectKeys.phases(projectId) });
  await queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId) });
  await queryClient.invalidateQueries({ queryKey: ['my-work'] });
}

// --------------------------------------------------------------------------------- task form

export function TaskForm({
  project,
  phases,
  members,
  task,
  onDone,
}: {
  project: ProjectDetail;
  phases: Phase[];
  members: LookupOption[];
  task: Task | null;
  onDone: () => void;
}) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const priorities = useEnumLookup('urgencies');
  const { register, handleSubmit, setError, watch, formState: { errors, isSubmitting, submitCount } } =
    useForm<TaskFormValues>({
      resolver: zodResolver(taskSchema),
      mode: 'onBlur',
      defaultValues: {
        phaseId: task ? String(task.phaseId) : '',
        title: task?.title ?? '',
        description: task?.description ?? '',
        assigneeId: task?.assigneeId ? String(task.assigneeId) : '',
        priority: task?.priority ?? 'NORMAL',
        dueDate: task?.dueDate ?? '',
      },
    });
  const feedback = useFormFeedback(setError, TASK_FIELDS);
  const phase = phases.find((p) => String(p.id) === watch('phaseId'));
  const openPhases = phases.filter((p) => p.status !== 'COMPLETED' || p.id === task?.phaseId);

  const onSubmit = async (v: TaskFormValues) => {
    feedback.clear();
    const payload = {
      phaseId: Number(v.phaseId),
      title: v.title,
      description: emptyToNull(v.description),
      assigneeId: v.assigneeId ? Number(v.assigneeId) : null,
      priority: v.priority,
      dueDate: v.dueDate,
    };
    try {
      if (task) {
        await tasksApi.update(task.id, payload);
      } else {
        await tasksApi.create(project.id, payload);
      }
      await refreshProjectTasks(queryClient, project.id);
      toast.success(task ? 'Task updated.' : 'Task created.');
      onDone();
    } catch (error) {
      feedback.handleError(error);
    }
  };

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <FormField label="Title" htmlFor="task-title" error={errors.title?.message} required>
        <Input id="task-title" invalid={!!errors.title} {...register('title')} />
      </FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Phase" htmlFor="task-phase" error={errors.phaseId?.message} required>
          <Select id="task-phase" options={openPhases.map((p) => ({ value: String(p.id), label: p.name }))} placeholder="Choose a phase" invalid={!!errors.phaseId} {...register('phaseId')} />
        </FormField>
        <FormField
          label="Due date"
          htmlFor="task-due"
          error={errors.dueDate?.message}
          hint={phase ? `Between ${formatDate(phase.startDate)} and ${formatDate(phase.endDate)}` : undefined}
          required
        >
          <Input id="task-due" type="date" min={phase?.startDate} max={phase?.endDate} invalid={!!errors.dueDate} {...register('dueDate')} />
        </FormField>
        <FormField label="Assignee" htmlFor="task-assignee" error={errors.assigneeId?.message}>
          <Select id="task-assignee" options={members} placeholder="Unassigned" {...register('assigneeId')} />
        </FormField>
        <FormField label="Priority" htmlFor="task-priority" error={errors.priority?.message} required>
          <Select id="task-priority" options={priorities.data ?? []} invalid={!!errors.priority} {...register('priority')} />
        </FormField>
      </div>
      <FormField label="Description" htmlFor="task-description" error={errors.description?.message}>
        <Textarea id="task-description" rows={3} maxLength={2000} {...register('description')} />
      </FormField>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" loading={isSubmitting}>
          {task ? 'Save task' : 'Create task'}
        </Button>
      </div>
    </form>
  );
}

// ----------------------------------------------------------------------------- sub-task form

function SubTaskForm({
  task,
  members,
  subTask,
  onDone,
}: {
  task: Task;
  members: LookupOption[];
  subTask: SubTask | null;
  onDone: (updated: Task) => void;
}) {
  const toast = useToast();
  const { register, handleSubmit, setError, reset, formState: { errors, isSubmitting, submitCount } } =
    useForm<SubTaskFormValues>({
      resolver: zodResolver(subTaskSchema),
      mode: 'onBlur',
      defaultValues: {
        title: subTask?.title ?? '',
        assigneeId: subTask?.assigneeId ? String(subTask.assigneeId) : '',
        dueDate: subTask?.dueDate ?? '',
      },
    });
  const feedback = useFormFeedback(setError, SUB_FIELDS);

  const onSubmit = async (v: SubTaskFormValues) => {
    feedback.clear();
    const payload = { title: v.title, assigneeId: v.assigneeId ? Number(v.assigneeId) : null, dueDate: emptyToNull(v.dueDate) };
    try {
      const updated = subTask ? await tasksApi.updateSubTask(subTask.id, payload) : await tasksApi.addSubTask(task.id, payload);
      toast.success(subTask ? 'Sub-task updated.' : 'Sub-task added.');
      reset({ title: '', assigneeId: '', dueDate: '' });
      onDone(updated);
    } catch (error) {
      feedback.handleError(error);
    }
  };

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-3 rounded-lg bg-surface p-3">
      <ErrorSummary messages={summary} />
      <div className="grid gap-3 sm:grid-cols-[2fr_1.5fr_1fr]">
        <FormField label="Sub-task" htmlFor="sub-title" error={errors.title?.message} required>
          <Input id="sub-title" invalid={!!errors.title} {...register('title')} />
        </FormField>
        <FormField label="Worker" htmlFor="sub-assignee" error={errors.assigneeId?.message}>
          <Select id="sub-assignee" options={members} placeholder="Unassigned" {...register('assigneeId')} />
        </FormField>
        <FormField label="Due" htmlFor="sub-due" error={errors.dueDate?.message}>
          <Input id="sub-due" type="date" invalid={!!errors.dueDate} {...register('dueDate')} />
        </FormField>
      </div>
      <Button type="submit" size="sm" loading={isSubmitting}>
        {subTask ? 'Save sub-task' : 'Add sub-task'}
      </Button>
    </form>
  );
}

// ---------------------------------------------------------------------------- task details

const NEXT_SUB_STATUS: Record<TaskStatus, TaskStatus> = { TODO: 'IN_PROGRESS', IN_PROGRESS: 'DONE', DONE: 'TODO' };

export function TaskDetails({
  task: initial,
  members,
  perms,
  onEdit,
  onDelete,
}: {
  task: Task;
  members: LookupOption[];
  perms: ProjectPermissions;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [task, setTask] = useState(initial);
  const [progress, setProgress] = useState(initial.progress);
  const [saving, setSaving] = useState(false);
  const [editingSub, setEditingSub] = useState<SubTask | null>(null);

  useEffect(() => {
    setTask(initial);
    setProgress(initial.progress);
  }, [initial]);

  const canUpdate = perms.canUpdateTask(task);

  const saveProgress = async (status: TaskStatus, value: number) => {
    setSaving(true);
    try {
      const updated = await tasksApi.progress(task.id, status, value);
      setTask(updated);
      setProgress(updated.progress);
      await refreshProjectTasks(queryClient, task.projectId);
      toast.success(updated.status === 'DONE' ? 'Task marked done.' : 'Progress saved.');
    } catch (error) {
      toast.error(toApiError(error).message);
    } finally {
      setSaving(false);
    }
  };

  const changeSub = async (sub: SubTask) => {
    try {
      await tasksApi.subTaskStatus(sub.id, NEXT_SUB_STATUS[sub.status]);
      setTask((t) => ({
        ...t,
        subTasks: t.subTasks.map((s) => (s.id === sub.id ? { ...s, status: NEXT_SUB_STATUS[sub.status] } : s)),
      }));
      await queryClient.invalidateQueries({ queryKey: projectKeys.tasks(task.projectId) });
    } catch (error) {
      toast.error(toApiError(error).message);
    }
  };

  const removeSub = async (sub: SubTask) => {
    try {
      setTask(await tasksApi.removeSubTask(sub.id));
      await queryClient.invalidateQueries({ queryKey: projectKeys.tasks(task.projectId) });
      toast.success('Sub-task removed.');
    } catch (error) {
      toast.error(toApiError(error).message);
    }
  };

  const onSubSaved = async (updated: Task) => {
    setTask(updated);
    setEditingSub(null);
    await queryClient.invalidateQueries({ queryKey: projectKeys.tasks(task.projectId) });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-2">
        <StatusBadge status={task.status} />
        <StatusBadge status={task.priority} label={`${humanize(task.priority)} priority`} />
        <span className="text-sm text-muted">
          {task.phaseName}, due {formatDate(task.dueDate)}
          {task.overdue && <span className="text-red-600">, overdue</span>}
        </span>
      </div>
      {task.description && <p className="whitespace-pre-line text-sm text-ink">{task.description}</p>}
      <p className="text-sm text-muted">Assigned to {task.assigneeName ?? 'nobody yet'}</p>

      <section>
        <h3 className="mb-2 text-sm font-semibold text-ink">Progress</h3>
        <ProgressBar value={progress} label="Task progress" />
        {canUpdate && (
          <div className="mt-3 flex flex-wrap items-center gap-3">
            <input
              type="range"
              min={0}
              max={100}
              step={5}
              value={progress}
              onChange={(e) => setProgress(Number(e.target.value))}
              className="w-48 accent-primary"
              aria-label="Progress percentage"
            />
            <Button size="sm" variant="secondary" loading={saving} onClick={() => saveProgress(progress === 0 ? 'TODO' : 'IN_PROGRESS', progress)}>
              Save {progress}%
            </Button>
            {task.status !== 'DONE' && (
              <Button size="sm" loading={saving} onClick={() => saveProgress('DONE', 100)}>
                Mark done
              </Button>
            )}
          </div>
        )}
      </section>

      <section>
        <h3 className="mb-2 text-sm font-semibold text-ink">Sub-tasks</h3>
        {task.subTasks.length === 0 ? (
          <p className="text-sm text-muted">No sub-tasks.</p>
        ) : (
          <ul className="divide-y divide-line rounded-lg border border-line">
            {task.subTasks.map((sub) => (
              <li key={sub.id} className="flex items-center gap-3 px-3 py-2.5">
                <button
                  type="button"
                  disabled={!perms.canUpdateSubTask(sub, task)}
                  onClick={() => changeSub(sub)}
                  className="text-primary disabled:cursor-default disabled:opacity-60"
                  aria-label={`Move ${sub.title} to ${humanize(NEXT_SUB_STATUS[sub.status])}`}
                  title={`Move to ${humanize(NEXT_SUB_STATUS[sub.status])}`}
                >
                  {sub.status === 'DONE' ? (
                    <CheckCircle2 className="h-5 w-5 text-green-600" aria-hidden />
                  ) : sub.status === 'IN_PROGRESS' ? (
                    <PlayCircle className="h-5 w-5 text-blue-600" aria-hidden />
                  ) : (
                    <Circle className="h-5 w-5" aria-hidden />
                  )}
                </button>
                <div className="min-w-0 flex-1">
                  <p className={`text-sm ${sub.status === 'DONE' ? 'text-muted line-through' : 'text-ink'}`}>{sub.title}</p>
                  <p className="text-xs text-muted">
                    {sub.assigneeName ?? 'Unassigned'}
                    {sub.dueDate ? `, due ${formatDate(sub.dueDate)}` : ''}
                  </p>
                </div>
                {perms.canPlanSubTasks && (
                  <>
                    <button type="button" onClick={() => setEditingSub(sub)} className="rounded-md p-1.5 text-muted hover:bg-surface hover:text-ink" aria-label={`Edit ${sub.title}`}>
                      <Pencil className="h-4 w-4" aria-hidden />
                    </button>
                    <button type="button" onClick={() => removeSub(sub)} className="rounded-md p-1.5 text-red-600 hover:bg-surface" aria-label={`Remove ${sub.title}`}>
                      <Trash2 className="h-4 w-4" aria-hidden />
                    </button>
                  </>
                )}
              </li>
            ))}
          </ul>
        )}
        {perms.canPlanSubTasks && task.status !== 'DONE' && (
          <div className="mt-3">
            <SubTaskForm key={editingSub?.id ?? 'new'} task={task} members={members} subTask={editingSub} onDone={onSubSaved} />
          </div>
        )}
      </section>

      {(perms.canCreateTasks || perms.canDeleteTasks) && (
        <div className="flex gap-2 border-t border-line pt-4">
          {perms.canCreateTasks && (
            <Button variant="secondary" size="sm" onClick={onEdit}>
              <Pencil className="h-4 w-4" aria-hidden />
              Edit task
            </Button>
          )}
          {perms.canDeleteTasks && (
            <Button variant="danger" size="sm" onClick={onDelete}>
              <Trash2 className="h-4 w-4" aria-hidden />
              Delete task
            </Button>
          )}
        </div>
      )}
    </div>
  );
}
