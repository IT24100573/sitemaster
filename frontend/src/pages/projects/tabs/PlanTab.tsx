import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, Circle, Flag, Pencil, Plus, Trash2 } from 'lucide-react';
import { projectsApi } from '../../../api/projectsApi';
import { useToast } from '../../../context/ToastContext';
import { useCurrency } from '../../../hooks/useCompanyProfile';
import { useFormFeedback } from '../../../hooks/useFormFeedback';
import { collectErrorMessages, toApiError } from '../../../lib/apiError';
import { formatDate, formatMoney } from '../../../lib/format';
import { emptyToNull } from '../../../lib/params';
import {
  milestoneSchema,
  phaseSchema,
  type MilestoneFormValues,
  type PhaseFormValues,
} from '../../../validation/projectSchemas';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog';
import { ErrorSummary } from '../../../components/ui/ErrorSummary';
import { FormField } from '../../../components/ui/FormField';
import { Input } from '../../../components/ui/Input';
import { Modal } from '../../../components/ui/Modal';
import { Select } from '../../../components/ui/Select';
import { Spinner } from '../../../components/ui/Spinner';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { Textarea } from '../../../components/ui/Textarea';
import { projectKeys } from '../queryKeys';
import type { ProjectPermissions } from '../useProjectPermissions';
import type { Milestone, Phase, ProjectDetail } from '../../../types/project';

const DAY = 86_400_000;
const PHASE_FIELDS = ['name', 'description', 'startDate', 'endDate', 'budget', 'sortOrder'] as const;
const MILESTONE_FIELDS = ['name', 'dueDate', 'phaseId'] as const;

function daysBetween(from: string, to: string) {
  return Math.round((new Date(`${to}T00:00:00`).getTime() - new Date(`${from}T00:00:00`).getTime()) / DAY);
}

// ---------------------------------------------------------------------------------- phase form

function PhaseForm({ project, phase, onDone }: { project: ProjectDetail; phase: Phase | null; onDone: () => void }) {
  const toast = useToast();
  const currency = useCurrency();
  const queryClient = useQueryClient();
  const { register, handleSubmit, setError, reset, formState: { errors, isSubmitting, submitCount } } =
    useForm<PhaseFormValues>({
      resolver: zodResolver(phaseSchema),
      mode: 'onBlur',
      defaultValues: { name: '', description: '', startDate: project.startDate, endDate: project.endDate, budget: '', sortOrder: '' },
    });
  const feedback = useFormFeedback(setError, PHASE_FIELDS);

  useEffect(() => {
    if (phase) {
      reset({
        name: phase.name,
        description: phase.description ?? '',
        startDate: phase.startDate,
        endDate: phase.endDate,
        budget: Number(phase.budget).toFixed(2),
        sortOrder: String(phase.sortOrder),
      });
    }
  }, [phase, reset]);

  const onSubmit = async (v: PhaseFormValues) => {
    feedback.clear();
    const payload = {
      name: v.name,
      description: emptyToNull(v.description),
      startDate: v.startDate,
      endDate: v.endDate,
      budget: v.budget,
      sortOrder: v.sortOrder === '' ? 0 : Number(v.sortOrder),
    };
    try {
      if (phase) {
        await projectsApi.updatePhase(phase.id, payload);
      } else {
        await projectsApi.createPhase(project.id, payload);
      }
      await queryClient.invalidateQueries({ queryKey: projectKeys.phases(project.id) });
      await queryClient.invalidateQueries({ queryKey: projectKeys.detail(project.id) });
      toast.success(phase ? 'Phase updated.' : 'Phase added.');
      onDone();
    } catch (error) {
      feedback.handleError(error);
    }
  };

  const remaining = Number(project.budget) - Number(project.allocatedBudget) + (phase ? Number(phase.budget) : 0);
  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <FormField label="Name" htmlFor="phase-name" error={errors.name?.message} required>
        <Input id="phase-name" invalid={!!errors.name} {...register('name')} />
      </FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Start" htmlFor="phase-start" error={errors.startDate?.message} required>
          <Input id="phase-start" type="date" min={project.startDate} max={project.endDate} invalid={!!errors.startDate} {...register('startDate')} />
        </FormField>
        <FormField label="End" htmlFor="phase-end" error={errors.endDate?.message} required>
          <Input id="phase-end" type="date" min={project.startDate} max={project.endDate} invalid={!!errors.endDate} {...register('endDate')} />
        </FormField>
        <FormField
          label={`Budget (${currency})`}
          htmlFor="phase-budget"
          error={errors.budget?.message}
          hint={`Up to ${formatMoney(remaining, currency)} available`}
          required
        >
          <Input id="phase-budget" inputMode="decimal" invalid={!!errors.budget} {...register('budget')} />
        </FormField>
        <FormField label="Order" htmlFor="phase-order" error={errors.sortOrder?.message} hint="Lower numbers come first">
          <Input id="phase-order" inputMode="numeric" invalid={!!errors.sortOrder} {...register('sortOrder')} />
        </FormField>
      </div>
      <FormField label="Description" htmlFor="phase-description" error={errors.description?.message}>
        <Textarea id="phase-description" rows={3} maxLength={2000} {...register('description')} />
      </FormField>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" loading={isSubmitting}>
          {phase ? 'Save phase' : 'Add phase'}
        </Button>
      </div>
    </form>
  );
}

// ----------------------------------------------------------------------------- milestone form

function MilestoneForm({
  project,
  phases,
  milestone,
  onDone,
}: {
  project: ProjectDetail;
  phases: Phase[];
  milestone: Milestone | null;
  onDone: () => void;
}) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } =
    useForm<MilestoneFormValues>({
      resolver: zodResolver(milestoneSchema),
      mode: 'onBlur',
      defaultValues: {
        name: milestone?.name ?? '',
        dueDate: milestone?.dueDate ?? '',
        phaseId: milestone?.phaseId ? String(milestone.phaseId) : '',
      },
    });
  const feedback = useFormFeedback(setError, MILESTONE_FIELDS);

  const onSubmit = async (v: MilestoneFormValues) => {
    feedback.clear();
    const payload = { name: v.name, dueDate: v.dueDate, phaseId: v.phaseId ? Number(v.phaseId) : null };
    try {
      if (milestone) {
        await projectsApi.updateMilestone(milestone.id, payload);
      } else {
        await projectsApi.createMilestone(project.id, payload);
      }
      await queryClient.invalidateQueries({ queryKey: projectKeys.milestones(project.id) });
      toast.success(milestone ? 'Milestone updated.' : 'Milestone added.');
      onDone();
    } catch (error) {
      feedback.handleError(error);
    }
  };

  const summary = submitCount > 0 ? [...collectErrorMessages(errors), ...feedback.serverMessages] : [];

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
      <ErrorSummary messages={summary} />
      <FormField label="Name" htmlFor="milestone-name" error={errors.name?.message} required>
        <Input id="milestone-name" invalid={!!errors.name} {...register('name')} />
      </FormField>
      <FormField label="Due date" htmlFor="milestone-date" error={errors.dueDate?.message} required>
        <Input id="milestone-date" type="date" min={project.startDate} max={project.endDate} invalid={!!errors.dueDate} {...register('dueDate')} />
      </FormField>
      <FormField label="Phase" htmlFor="milestone-phase" error={errors.phaseId?.message}>
        <Select id="milestone-phase" options={phases.map((p) => ({ value: String(p.id), label: p.name }))} placeholder="Whole project" {...register('phaseId')} />
      </FormField>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" loading={isSubmitting}>
          {milestone ? 'Save milestone' : 'Add milestone'}
        </Button>
      </div>
    </form>
  );
}

// ---------------------------------------------------------------------------------------- tab

type Deleting = { kind: 'phase'; item: Phase } | { kind: 'milestone'; item: Milestone } | null;

/** Phases on a simple timeline, plus milestones. */
export function PlanTab({ project, perms }: { project: ProjectDetail; perms: ProjectPermissions }) {
  const toast = useToast();
  const currency = useCurrency();
  const queryClient = useQueryClient();
  const [phaseModal, setPhaseModal] = useState<{ phase: Phase | null } | null>(null);
  const [milestoneModal, setMilestoneModal] = useState<{ milestone: Milestone | null } | null>(null);
  const [deleting, setDeleting] = useState<Deleting>(null);

  const phases = useQuery({ queryKey: projectKeys.phases(project.id), queryFn: () => projectsApi.phases(project.id) });
  const milestones = useQuery({
    queryKey: projectKeys.milestones(project.id),
    queryFn: () => projectsApi.milestones(project.id),
  });

  const span = Math.max(1, daysBetween(project.startDate, project.endDate));

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['projects', project.id] });
    await queryClient.invalidateQueries({ queryKey: projectKeys.detail(project.id) });
  };

  const phaseStatus = useMutation({
    mutationFn: ({ phase, status }: { phase: Phase; status: string }) => projectsApi.changePhaseStatus(phase.id, status),
    onSuccess: async (updated) => {
      await refresh();
      toast.success(`${updated.name} updated.`);
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const toggleMilestone = useMutation({
    mutationFn: (m: Milestone) => projectsApi.completeMilestone(m.id, !m.completed),
    onSuccess: async (m) => {
      await refresh();
      toast.success(m.completed ? `${m.name} completed.` : `${m.name} reopened.`);
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const remove = useMutation({
    mutationFn: async (target: NonNullable<Deleting>) => {
      if (target.kind === 'phase') {
        await projectsApi.deletePhase(target.item.id);
      } else {
        await projectsApi.deleteMilestone(target.item.id);
      }
      return target;
    },
    onSuccess: async (target) => {
      setDeleting(null);
      await refresh();
      toast.success(`${target.item.name} deleted.`);
    },
    onError: (error) => {
      setDeleting(null);
      toast.error(toApiError(error).message);
    },
  });

  return (
    <div className="space-y-6">
      <Card
        title="Phases"
        actions={
          perms.canManage && (
            <Button size="sm" onClick={() => setPhaseModal({ phase: null })}>
              <Plus className="h-4 w-4" aria-hidden />
              Add phase
            </Button>
          )
        }
      >
        {phases.isLoading ? (
          <Spinner />
        ) : phases.data && phases.data.length > 0 ? (
          <ul className="space-y-5">
            {phases.data.map((phase) => {
              const left = (daysBetween(project.startDate, phase.startDate) / span) * 100;
              const width = Math.max(1.5, ((daysBetween(phase.startDate, phase.endDate) + 1) / span) * 100);
              return (
                <li key={phase.id}>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="font-medium text-ink">{phase.name}</span>
                    <StatusBadge status={phase.status} />
                    <span className="text-xs text-muted">
                      {formatDate(phase.startDate)} - {formatDate(phase.endDate)}, {formatMoney(phase.budget, currency)},{' '}
                      {phase.openTaskCount}/{phase.taskCount} tasks open
                    </span>
                    {perms.canManage && (
                      <span className="ml-auto flex items-center gap-1">
                        {phase.status !== 'COMPLETED' && (
                          <Button size="sm" variant="ghost" onClick={() => phaseStatus.mutate({ phase, status: 'COMPLETED' })}>
                            Complete
                          </Button>
                        )}
                        {phase.status === 'COMPLETED' && (
                          <Button size="sm" variant="ghost" onClick={() => phaseStatus.mutate({ phase, status: 'IN_PROGRESS' })}>
                            Reopen
                          </Button>
                        )}
                        <button type="button" onClick={() => setPhaseModal({ phase })} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Edit ${phase.name}`}>
                          <Pencil className="h-4 w-4" aria-hidden />
                        </button>
                        <button type="button" onClick={() => setDeleting({ kind: 'phase', item: phase })} className="rounded-md p-2 text-red-600 hover:bg-surface" aria-label={`Delete ${phase.name}`}>
                          <Trash2 className="h-4 w-4" aria-hidden />
                        </button>
                      </span>
                    )}
                  </div>
                  <div className="relative mt-2 h-3 rounded-full bg-surface ring-1 ring-inset ring-line">
                    <div
                      className={`absolute inset-y-0 rounded-full ${phase.status === 'COMPLETED' ? 'bg-green-500' : 'bg-primary'}`}
                      style={{ left: `${Math.max(0, left)}%`, width: `${Math.min(width, 100 - Math.max(0, left))}%` }}
                    />
                  </div>
                </li>
              );
            })}
          </ul>
        ) : (
          <p className="text-sm text-muted">No phases yet.{perms.canManage ? ' Add the first phase to start planning tasks.' : ''}</p>
        )}
      </Card>

      <Card
        title="Milestones"
        actions={
          perms.canManage && (
            <Button size="sm" onClick={() => setMilestoneModal({ milestone: null })}>
              <Plus className="h-4 w-4" aria-hidden />
              Add milestone
            </Button>
          )
        }
      >
        {milestones.isLoading ? (
          <Spinner />
        ) : milestones.data && milestones.data.length > 0 ? (
          <ul className="divide-y divide-line">
            {milestones.data.map((m) => (
              <li key={m.id} className="flex items-center gap-3 py-3">
                <button
                  type="button"
                  disabled={!perms.canManage}
                  onClick={() => toggleMilestone.mutate(m)}
                  className="rounded-full text-primary disabled:cursor-default"
                  aria-label={m.completed ? `Reopen ${m.name}` : `Complete ${m.name}`}
                >
                  {m.completed ? <CheckCircle2 className="h-5 w-5 text-green-600" aria-hidden /> : <Circle className="h-5 w-5" aria-hidden />}
                </button>
                <div className="min-w-0 flex-1">
                  <p className={`text-sm font-medium ${m.completed ? 'text-muted line-through' : 'text-ink'}`}>
                    <Flag className="mr-1 inline h-3.5 w-3.5 text-primary" aria-hidden />
                    {m.name}
                  </p>
                  <p className={`text-xs ${m.overdue ? 'text-red-600' : 'text-muted'}`}>
                    Due {formatDate(m.dueDate)}
                    {m.phaseName ? `, ${m.phaseName}` : ''}
                    {m.overdue ? ', overdue' : ''}
                    {m.completedDate ? `, completed ${formatDate(m.completedDate)}` : ''}
                  </p>
                </div>
                {perms.canManage && (
                  <>
                    <button type="button" onClick={() => setMilestoneModal({ milestone: m })} className="rounded-md p-2 text-muted hover:bg-surface hover:text-ink" aria-label={`Edit ${m.name}`}>
                      <Pencil className="h-4 w-4" aria-hidden />
                    </button>
                    <button type="button" onClick={() => setDeleting({ kind: 'milestone', item: m })} className="rounded-md p-2 text-red-600 hover:bg-surface" aria-label={`Delete ${m.name}`}>
                      <Trash2 className="h-4 w-4" aria-hidden />
                    </button>
                  </>
                )}
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-sm text-muted">No milestones yet.</p>
        )}
      </Card>

      <Modal open={phaseModal !== null} title={phaseModal?.phase ? 'Edit phase' : 'Add phase'} onClose={() => setPhaseModal(null)}>
        {phaseModal && <PhaseForm project={project} phase={phaseModal.phase} onDone={() => setPhaseModal(null)} />}
      </Modal>
      <Modal open={milestoneModal !== null} title={milestoneModal?.milestone ? 'Edit milestone' : 'Add milestone'} onClose={() => setMilestoneModal(null)}>
        {milestoneModal && (
          <MilestoneForm project={project} phases={phases.data ?? []} milestone={milestoneModal.milestone} onDone={() => setMilestoneModal(null)} />
        )}
      </Modal>
      <ConfirmDialog
        open={deleting !== null}
        title={`Delete ${deleting?.item.name ?? ''}?`}
        message={deleting?.kind === 'phase' ? 'Only phases without tasks can be deleted.' : 'The milestone is removed from the timeline.'}
        confirmLabel="Delete"
        tone="danger"
        loading={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting)}
        onCancel={() => setDeleting(null)}
      />
    </div>
  );
}
