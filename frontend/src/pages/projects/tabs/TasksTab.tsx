import { useMemo, useState, type DragEvent } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Columns3, List, Plus } from 'lucide-react';
import { projectsApi } from '../../../api/projectsApi';
import { tasksApi } from '../../../api/tasksApi';
import { useToast } from '../../../context/ToastContext';
import { useEnumLookup } from '../../../hooks/useLookup';
import { useTableState } from '../../../hooks/useTableState';
import { toApiError } from '../../../lib/apiError';
import { formatDate, humanize } from '../../../lib/format';
import { Alert } from '../../../components/ui/Alert';
import { Button } from '../../../components/ui/Button';
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog';
import { DataTable, type Column } from '../../../components/ui/DataTable';
import { Modal } from '../../../components/ui/Modal';
import { ProgressBar } from '../../../components/ui/ProgressBar';
import { SearchInput } from '../../../components/ui/SearchInput';
import { Select } from '../../../components/ui/Select';
import { Spinner } from '../../../components/ui/Spinner';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { projectKeys } from '../queryKeys';
import { refreshProjectTasks, TaskDetails, TaskForm } from './TaskDialogs';
import type { ProjectPermissions } from '../useProjectPermissions';
import type { ProjectDetail } from '../../../types/project';
import type { Task, TaskFilters, TaskStatus } from '../../../types/task';

const BOARD_SIZE = 100;
const COLUMNS: TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

/** Progress to send when a card is dropped into a column. */
function progressFor(task: Task, status: TaskStatus) {
  if (status === 'DONE') {
    return 100;
  }
  if (status === 'TODO') {
    return 0;
  }
  return task.progress > 0 && task.progress < 100 ? task.progress : 10;
}

/** Tasks of a project as a sortable list or a Kanban board. */
export function TasksTab({ project, perms }: { project: ProjectDetail; perms: ProjectPermissions }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const table = useTableState('dueDate');
  const [view, setView] = useState<'list' | 'board'>('list');
  const [filters, setFilters] = useState<TaskFilters>({});
  const [formTask, setFormTask] = useState<{ task: Task | null } | null>(null);
  const [openTask, setOpenTask] = useState<Task | null>(null);
  const [deleting, setDeleting] = useState<Task | null>(null);
  const [dragOver, setDragOver] = useState<TaskStatus | null>(null);
  const statuses = useEnumLookup('task-statuses');

  const phases = useQuery({ queryKey: projectKeys.phases(project.id), queryFn: () => projectsApi.phases(project.id) });
  const members = useQuery({ queryKey: projectKeys.members(project.id), queryFn: () => projectsApi.members(project.id) });
  const memberOptions = useMemo(
    () => (members.data ?? []).map((m) => ({ value: String(m.userId), label: `${m.fullName} (${m.roleDisplayName})` })),
    [members.data],
  );

  const params = view === 'board' ? { ...table.params, page: 0, size: BOARD_SIZE } : table.params;
  const tasks = useQuery({
    queryKey: [...projectKeys.tasks(project.id), params, filters, view],
    queryFn: () => tasksApi.list(project.id, params, view === 'board' ? { ...filters, status: undefined } : filters),
    placeholderData: keepPreviousData,
  });

  const move = useMutation({
    mutationFn: ({ task, status }: { task: Task; status: TaskStatus }) =>
      tasksApi.progress(task.id, status, progressFor(task, status)),
    onSuccess: async (updated) => {
      await refreshProjectTasks(queryClient, project.id);
      toast.success(`${updated.title} moved to ${humanize(updated.status).toLowerCase()}.`);
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const remove = useMutation({
    mutationFn: (task: Task) => tasksApi.remove(task.id),
    onSuccess: async () => {
      setDeleting(null);
      setOpenTask(null);
      await refreshProjectTasks(queryClient, project.id);
      toast.success('Task deleted.');
    },
    onError: (error) => {
      setDeleting(null);
      toast.error(toApiError(error).message);
    },
  });

  const setFilter = (key: keyof TaskFilters, value: string) => {
    setFilters((current) => ({ ...current, [key]: value || undefined }));
    table.resetPage();
  };

  const onDrop = (event: DragEvent<HTMLDivElement>, status: TaskStatus) => {
    event.preventDefault();
    setDragOver(null);
    const id = Number(event.dataTransfer.getData('text/plain'));
    const task = tasks.data?.content.find((t) => t.id === id);
    if (task && task.status !== status && perms.canUpdateTask(task)) {
      move.mutate({ task, status });
    }
  };

  const columns: Column<Task>[] = [
    {
      key: 'title',
      header: 'Task',
      sortKey: 'title',
      render: (t) => (
        <button type="button" onClick={() => setOpenTask(t)} className="text-left">
          <span className="font-medium text-ink hover:text-primary">{t.title}</span>
          <span className="block text-xs text-muted">
            {t.phaseName}
            {t.subTasks.length > 0 ? `, ${t.subTasks.filter((s) => s.status === 'DONE').length}/${t.subTasks.length} sub-tasks` : ''}
          </span>
        </button>
      ),
    },
    { key: 'assignee', header: 'Assignee', render: (t) => t.assigneeName ?? <span className="text-muted">Unassigned</span> },
    { key: 'priority', header: 'Priority', sortKey: 'priority', render: (t) => <StatusBadge status={t.priority} /> },
    {
      key: 'due',
      header: 'Due',
      sortKey: 'dueDate',
      className: 'whitespace-nowrap',
      render: (t) => <span className={t.overdue ? 'font-medium text-red-600' : ''}>{formatDate(t.dueDate)}</span>,
    },
    { key: 'status', header: 'Status', sortKey: 'status', render: (t) => <StatusBadge status={t.status} /> },
    { key: 'progress', header: 'Progress', sortKey: 'progress', className: 'w-40', render: (t) => <ProgressBar value={t.progress} label={`${t.title} progress`} /> },
  ];

  const toolbar = (
    <>
      <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Task or assignee" />
      <div className="flex flex-wrap gap-2 sm:ml-auto">
        <Select aria-label="Filter by phase" options={(phases.data ?? []).map((p) => ({ value: String(p.id), label: p.name }))} placeholder="All phases" value={filters.phaseId ?? ''} onChange={(e) => setFilter('phaseId', e.target.value)} />
        {view === 'list' && (
          <Select aria-label="Filter by status" options={statuses.data ?? []} placeholder="All statuses" value={filters.status ?? ''} onChange={(e) => setFilter('status', e.target.value)} />
        )}
        <Select aria-label="Filter by assignee" options={memberOptions} placeholder="Anyone" value={filters.assigneeId ?? ''} onChange={(e) => setFilter('assigneeId', e.target.value)} />
      </div>
    </>
  );

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="inline-flex rounded-lg border border-line bg-white p-1 text-sm" role="tablist" aria-label="Task view">
          {[
            { id: 'list' as const, label: 'List', icon: List },
            { id: 'board' as const, label: 'Board', icon: Columns3 },
          ].map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              type="button"
              role="tab"
              aria-selected={view === id}
              onClick={() => setView(id)}
              className={`inline-flex items-center gap-1.5 rounded-md px-3 py-1.5 font-medium ${view === id ? 'bg-primary-light text-primary-dark' : 'text-muted hover:text-ink'}`}
            >
              <Icon className="h-4 w-4" aria-hidden />
              {label}
            </button>
          ))}
        </div>
        {perms.canCreateTasks && (
          <Button onClick={() => setFormTask({ task: null })} disabled={!phases.data?.length} title={phases.data?.length ? undefined : 'Add a phase first'}>
            <Plus className="h-4 w-4" aria-hidden />
            New task
          </Button>
        )}
      </div>

      {perms.canCreateTasks && phases.data?.length === 0 && (
        <Alert tone="info">Tasks belong to phases. Add a phase on the Timeline tab first.</Alert>
      )}

      {view === 'list' ? (
        <DataTable
          columns={columns}
          page={tasks.data}
          rowKey={(t) => t.id}
          loading={tasks.isFetching}
          error={tasks.isError ? toApiError(tasks.error).message : null}
          emptyMessage="No tasks match these filters."
          sort={table.sort}
          direction={table.direction}
          onSort={table.toggleSort}
          onPageChange={table.setPage}
          toolbar={toolbar}
        />
      ) : (
        <div className="space-y-3">
          <div className="card flex flex-col gap-3 p-4 sm:flex-row sm:items-center">{toolbar}</div>
          {tasks.data && tasks.data.totalElements > BOARD_SIZE && (
            <Alert tone="info">The board shows the first {BOARD_SIZE} tasks. Use the filters or the list view for the rest.</Alert>
          )}
          {tasks.isLoading ? (
            <Spinner />
          ) : (
            <div className="grid gap-4 md:grid-cols-3">
              {COLUMNS.map((status) => {
                const cards = (tasks.data?.content ?? []).filter((t) => t.status === status);
                return (
                  <div
                    key={status}
                    onDragOver={(e) => {
                      e.preventDefault();
                      setDragOver(status);
                    }}
                    onDragLeave={() => setDragOver(null)}
                    onDrop={(e) => onDrop(e, status)}
                    className={`min-h-[200px] rounded-card border-2 border-dashed p-3 transition-colors ${dragOver === status ? 'border-primary bg-primary-light' : 'border-transparent bg-surface'}`}
                    aria-label={`${humanize(status)} column`}
                  >
                    <h3 className="mb-3 flex items-center justify-between text-sm font-semibold text-ink">
                      {humanize(status)}
                      <span className="rounded-full bg-white px-2 py-0.5 text-xs text-muted">{cards.length}</span>
                    </h3>
                    <ul className="space-y-2">
                      {cards.map((t) => {
                        const draggable = perms.canUpdateTask(t);
                        return (
                          <li
                            key={t.id}
                            draggable={draggable}
                            onDragStart={(e) => e.dataTransfer.setData('text/plain', String(t.id))}
                            className={`card p-3 ${draggable ? 'cursor-grab active:cursor-grabbing' : ''}`}
                          >
                            <button type="button" onClick={() => setOpenTask(t)} className="block w-full text-left">
                              <p className="text-sm font-medium text-ink">{t.title}</p>
                              <p className="mt-0.5 text-xs text-muted">
                                {t.assigneeName ?? 'Unassigned'}, due{' '}
                                <span className={t.overdue ? 'text-red-600' : ''}>{formatDate(t.dueDate)}</span>
                              </p>
                              <div className="mt-2">
                                <ProgressBar value={t.progress} label={`${t.title} progress`} />
                              </div>
                            </button>
                          </li>
                        );
                      })}
                    </ul>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      <Modal open={formTask !== null} title={formTask?.task ? 'Edit task' : 'New task'} onClose={() => setFormTask(null)} size="lg">
        {formTask && (
          <TaskForm
            project={project}
            phases={phases.data ?? []}
            members={memberOptions}
            task={formTask.task}
            onDone={() => {
              setFormTask(null);
              setOpenTask(null);
            }}
          />
        )}
      </Modal>

      <Modal open={openTask !== null} title={openTask?.title ?? ''} onClose={() => setOpenTask(null)} size="lg">
        {openTask && (
          <TaskDetails
            task={openTask}
            members={memberOptions}
            perms={perms}
            onEdit={() => setFormTask({ task: openTask })}
            onDelete={() => setDeleting(openTask)}
          />
        )}
      </Modal>

      <ConfirmDialog
        open={deleting !== null}
        title={`Delete ${deleting?.title ?? 'task'}?`}
        message="The task and its sub-tasks are removed and project progress is recalculated."
        confirmLabel="Delete task"
        tone="danger"
        loading={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting)}
        onCancel={() => setDeleting(null)}
      />
    </div>
  );
}
