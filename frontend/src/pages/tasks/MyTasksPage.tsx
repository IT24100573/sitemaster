import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CheckCircle2, PlayCircle } from 'lucide-react';
import { tasksApi } from '../../api/tasksApi';
import { useAuth } from '../../context/AuthContext';
import { useToast } from '../../context/ToastContext';
import { toApiError } from '../../lib/apiError';
import { formatDate } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { PageHeader } from '../../components/ui/PageHeader';
import { ProgressBar } from '../../components/ui/ProgressBar';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import type { SubTask, Task } from '../../types/task';

const MY_WORK_KEY = ['my-work'] as const;

/** Everything assigned to the signed-in user, with one-tap start/complete. */
export default function MyTasksPage() {
  const toast = useToast();
  const { hasPermission } = useAuth();
  const canUpdate = hasPermission('TASK:EDIT');
  const queryClient = useQueryClient();
  const work = useQuery({ queryKey: MY_WORK_KEY, queryFn: tasksApi.mine });

  const refresh = () => queryClient.invalidateQueries({ queryKey: MY_WORK_KEY });

  const updateTask = useMutation({
    mutationFn: ({ task, done }: { task: Task; done: boolean }) =>
      tasksApi.progress(task.id, done ? 'DONE' : 'IN_PROGRESS', done ? 100 : Math.max(task.progress, 10)),
    onSuccess: (t) => {
      refresh();
      toast.success(t.status === 'DONE' ? `${t.title} completed.` : `${t.title} started.`);
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const updateSub = useMutation({
    mutationFn: ({ sub, done }: { sub: SubTask; done: boolean }) => tasksApi.subTaskStatus(sub.id, done ? 'DONE' : 'IN_PROGRESS'),
    onSuccess: (s) => {
      refresh();
      toast.success(s.status === 'DONE' ? `${s.title} completed.` : `${s.title} started.`);
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  if (work.isLoading) {
    return <FullPageSpinner />;
  }
  if (work.isError || !work.data) {
    return <Alert tone="error">{toApiError(work.error).message}</Alert>;
  }

  const actions = (status: string, onStart: () => void, onDone: () => void, busy: boolean) =>
    canUpdate &&
    status !== 'DONE' && (
      <div className="flex gap-2">
        {status === 'TODO' && (
          <Button size="sm" variant="secondary" onClick={onStart} disabled={busy}>
            <PlayCircle className="h-4 w-4" aria-hidden />
            Start
          </Button>
        )}
        <Button size="sm" onClick={onDone} disabled={busy}>
          <CheckCircle2 className="h-4 w-4" aria-hidden />
          Complete
        </Button>
      </div>
    );

  const openSubs = work.data.subTasks.filter((s) => s.status !== 'DONE');
  const doneSubs = work.data.subTasks.filter((s) => s.status === 'DONE');

  return (
    <>
      <PageHeader title="My tasks" description="Work assigned to you on all your projects." />
      <div className="space-y-6">
        <Card title="Tasks" description="Tasks you are responsible for.">
          {work.data.tasks.length === 0 ? (
            <p className="text-sm text-muted">No tasks assigned to you.</p>
          ) : (
            <ul className="divide-y divide-line">
              {work.data.tasks.map((t) => (
                <li key={t.id} className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center">
                  <div className="min-w-0 flex-1">
                    <p className="font-medium text-ink">{t.title}</p>
                    <p className="text-xs text-muted">
                      <Link to={`../projects/${t.projectId}?tab=tasks`} relative="path" className="text-primary hover:text-primary-dark">
                        {t.projectName}
                      </Link>
                      , {t.phaseName}, due <span className={t.overdue ? 'text-red-600' : ''}>{formatDate(t.dueDate)}</span>
                    </p>
                    <div className="mt-2 max-w-xs">
                      <ProgressBar value={t.progress} label={`${t.title} progress`} />
                    </div>
                  </div>
                  <StatusBadge status={t.status} />
                  {actions(
                    t.status,
                    () => updateTask.mutate({ task: t, done: false }),
                    () => updateTask.mutate({ task: t, done: true }),
                    updateTask.isPending,
                  )}
                </li>
              ))}
            </ul>
          )}
        </Card>

        <Card title="Sub-tasks" description="Pieces of work your site engineer gave you.">
          {openSubs.length === 0 && doneSubs.length === 0 ? (
            <p className="text-sm text-muted">No sub-tasks assigned to you.</p>
          ) : (
            <ul className="divide-y divide-line">
              {[...openSubs, ...doneSubs].map((s) => (
                <li key={s.id} className="flex flex-col gap-3 py-4 sm:flex-row sm:items-center">
                  <div className="min-w-0 flex-1">
                    <p className={`font-medium ${s.status === 'DONE' ? 'text-muted line-through' : 'text-ink'}`}>{s.title}</p>
                    <p className="text-xs text-muted">
                      {s.projectName}, part of {s.taskTitle}
                      {s.dueDate ? `, due ${formatDate(s.dueDate)}` : ''}
                    </p>
                  </div>
                  <StatusBadge status={s.status} />
                  {actions(
                    s.status,
                    () => updateSub.mutate({ sub: s, done: false }),
                    () => updateSub.mutate({ sub: s, done: true }),
                    updateSub.isPending,
                  )}
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>
    </>
  );
}
