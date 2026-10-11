import { useState } from 'react';
import { Link, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Pencil } from 'lucide-react';
import { projectsApi } from '../../api/projectsApi';
import { useToast } from '../../context/ToastContext';
import { toApiError } from '../../lib/apiError';
import { humanize } from '../../lib/format';
import { Alert } from '../../components/ui/Alert';
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';
import { PageHeader } from '../../components/ui/PageHeader';
import { FullPageSpinner } from '../../components/ui/Spinner';
import { StatusBadge } from '../../components/ui/StatusBadge';
import { Tabs, type TabItem } from '../../components/ui/Tabs';
import { useProjectPermissions } from './useProjectPermissions';
import { projectKeys } from './queryKeys';
import { OverviewTab } from './tabs/OverviewTab';
import { TasksTab } from './tabs/TasksTab';
import { TeamTab } from './tabs/TeamTab';
import { PlanTab } from './tabs/PlanTab';
import { DocumentsTab } from './tabs/DocumentsTab';
import { IssuesTab } from './tabs/IssuesTab';
import { BudgetTab } from './tabs/BudgetTab';
import type { ProjectDetail, ProjectStatus } from '../../types/project';

const STATUS_CONFIRM: Partial<Record<ProjectStatus, string>> = {
  ACTIVE: 'Work on site can be recorded once the project is active.',
  DELAYED: 'The project will show as delayed on dashboards and the public map.',
  COMPLETED: 'Progress is set to 100% and the project appears in the public gallery.',
  ARCHIVED: 'Archived projects become read-only and leave every list. This cannot be undone.',
};

function ProjectBody({ project }: { project: ProjectDetail }) {
  const perms = useProjectPermissions(project);
  const toast = useToast();
  const queryClient = useQueryClient();
  const [params, setParams] = useSearchParams();
  const [target, setTarget] = useState<ProjectStatus | null>(null);

  const tabs: TabItem[] = [
    { id: 'overview', label: 'Overview' },
    ...(perms.canViewTasks ? [{ id: 'tasks', label: 'Tasks', count: project.taskCount }] : []),
    { id: 'team', label: 'Team', count: project.memberCount },
    { id: 'timeline', label: 'Timeline' },
    ...(perms.canViewDocuments ? [{ id: 'documents', label: 'Documents' }] : []),
    ...(perms.canViewIssues ? [{ id: 'issues', label: 'Issues & risks', count: project.openIssueCount }] : []),
    ...(perms.canSeeBudget ? [{ id: 'budget', label: 'Budget' }] : []),
  ];
  const requested = params.get('tab') ?? 'overview';
  const active = tabs.some((t) => t.id === requested) ? requested : 'overview';

  const changeStatus = useMutation({
    mutationFn: (status: ProjectStatus) => projectsApi.changeStatus(project.id, status),
    onSuccess: async (updated) => {
      queryClient.setQueryData(projectKeys.detail(project.id), updated);
      await queryClient.invalidateQueries({ queryKey: ['projects'], exact: false });
      setTarget(null);
      toast.success(`${updated.name} is now ${humanize(updated.status).toLowerCase()}.`);
    },
    onError: (error) => {
      setTarget(null);
      toast.error(toApiError(error).message);
    },
  });

  return (
    <>
      <Link to=".." relative="path" className="mb-4 inline-flex items-center gap-1 text-sm font-medium text-primary hover:text-primary-dark">
        <ArrowLeft className="h-4 w-4" aria-hidden />
        Back to projects
      </Link>
      <PageHeader
        title={project.name}
        description={
          <span className="flex flex-wrap items-center gap-2">
            <StatusBadge status={project.status} />
            <span>
              {project.clientName}, managed by {project.managerName}
            </span>
          </span>
        }
        actions={
          perms.canManage && (
            <>
              {project.nextStatuses.map((status) => (
                <button
                  key={status}
                  type="button"
                  onClick={() => setTarget(status)}
                  className={`inline-flex h-11 items-center rounded-lg border px-4 text-sm font-medium ${
                    status === 'ARCHIVED' || status === 'DELAYED'
                      ? 'border-red-200 text-red-700 hover:bg-red-50'
                      : 'border-line bg-white text-ink hover:bg-surface'
                  }`}
                >
                  Mark {humanize(status).toLowerCase()}
                </button>
              ))}
              <Link to="edit" className="inline-flex h-11 items-center gap-2 rounded-lg bg-primary px-4 text-sm font-medium text-white hover:bg-primary-dark">
                <Pencil className="h-4 w-4" aria-hidden />
                Edit
              </Link>
            </>
          )
        }
      />

      {project.status === 'ARCHIVED' && (
        <div className="mb-6">
          <Alert tone="info">This project is archived. Everything is read-only.</Alert>
        </div>
      )}

      <Tabs tabs={tabs} active={active} onChange={(id) => setParams({ tab: id }, { replace: true })} label="Project sections" />

      {active === 'overview' && <OverviewTab project={project} perms={perms} />}
      {active === 'tasks' && <TasksTab project={project} perms={perms} />}
      {active === 'team' && <TeamTab project={project} perms={perms} />}
      {active === 'timeline' && <PlanTab project={project} perms={perms} />}
      {active === 'documents' && <DocumentsTab project={project} perms={perms} />}
      {active === 'issues' && <IssuesTab project={project} perms={perms} />}
      {active === 'budget' && <BudgetTab project={project} />}

      <ConfirmDialog
        open={target !== null}
        title={target ? `Mark ${project.name} as ${humanize(target).toLowerCase()}?` : ''}
        message={target ? STATUS_CONFIRM[target] : ''}
        confirmLabel={target ? `Mark ${humanize(target).toLowerCase()}` : 'Confirm'}
        tone={target === 'ARCHIVED' || target === 'DELAYED' ? 'danger' : 'primary'}
        loading={changeStatus.isPending}
        onConfirm={() => target && changeStatus.mutate(target)}
        onCancel={() => setTarget(null)}
      />
    </>
  );
}

/** Project page with tabs for overview, tasks, team, timeline, documents, issues and budget. */
export default function ProjectDetailPage() {
  const { id } = useParams();
  const projectId = Number(id);
  const project = useQuery({ queryKey: projectKeys.detail(projectId), queryFn: () => projectsApi.get(projectId) });

  if (project.isLoading) {
    return <FullPageSpinner />;
  }
  if (project.isError || !project.data) {
    return <Alert tone="error">{toApiError(project.error).message}</Alert>;
  }
  return <ProjectBody project={project.data} />;
}
