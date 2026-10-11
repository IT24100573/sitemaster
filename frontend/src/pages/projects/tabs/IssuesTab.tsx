import { useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { issuesApi } from '../../../api/issuesApi';
import { projectsApi } from '../../../api/projectsApi';
import { useToast } from '../../../context/ToastContext';
import { useFormFeedback } from '../../../hooks/useFormFeedback';
import { useEnumLookup } from '../../../hooks/useLookup';
import { useTableState } from '../../../hooks/useTableState';
import { collectErrorMessages, toApiError } from '../../../lib/apiError';
import { formatDate, formatDateTime, humanize } from '../../../lib/format';
import { emptyToNull } from '../../../lib/params';
import { CLIENT_MESSAGES } from '../../../validation/messages';
import { issueSchema, type IssueFormValues } from '../../../validation/projectSchemas';
import { Button } from '../../../components/ui/Button';
import { DataTable, type Column } from '../../../components/ui/DataTable';
import { DetailList } from '../../../components/ui/DetailList';
import { ErrorSummary } from '../../../components/ui/ErrorSummary';
import { FormField } from '../../../components/ui/FormField';
import { Input } from '../../../components/ui/Input';
import { Modal } from '../../../components/ui/Modal';
import { SearchInput } from '../../../components/ui/SearchInput';
import { Select } from '../../../components/ui/Select';
import { StatusBadge } from '../../../components/ui/StatusBadge';
import { Textarea } from '../../../components/ui/Textarea';
import { projectKeys } from '../queryKeys';
import type { ProjectPermissions } from '../useProjectPermissions';
import type { LookupOption } from '../../../types/common';
import type { ProjectDetail } from '../../../types/project';
import type { Issue, IssueFilters } from '../../../types/issue';

const FIELDS = ['type', 'title', 'description', 'severity', 'assignedToId', 'dueDate'] as const;

function IssueForm({ projectId, members, issue, onDone }: { projectId: number; members: LookupOption[]; issue: Issue | null; onDone: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const types = useEnumLookup('issue-types');
  const severities = useEnumLookup('issue-severities');
  const { register, handleSubmit, setError, formState: { errors, isSubmitting, submitCount } } = useForm<IssueFormValues>({
    resolver: zodResolver(issueSchema),
    mode: 'onBlur',
    defaultValues: {
      type: issue?.type ?? 'ISSUE',
      title: issue?.title ?? '',
      description: issue?.description ?? '',
      severity: issue?.severity ?? 'MEDIUM',
      assignedToId: issue?.assignedToId ? String(issue.assignedToId) : '',
      dueDate: issue?.dueDate ?? '',
    },
  });
  const feedback = useFormFeedback(setError, FIELDS);

  const onSubmit = async (v: IssueFormValues) => {
    feedback.clear();
    const payload = {
      type: v.type,
      title: v.title,
      description: v.description,
      severity: v.severity,
      assignedToId: v.assignedToId ? Number(v.assignedToId) : null,
      dueDate: emptyToNull(v.dueDate),
    };
    try {
      if (issue) {
        await issuesApi.update(issue.id, payload);
      } else {
        await issuesApi.report(projectId, payload);
      }
      await queryClient.invalidateQueries({ queryKey: projectKeys.issues(projectId) });
      await queryClient.invalidateQueries({ queryKey: projectKeys.detail(projectId) });
      toast.success(issue ? 'Saved.' : 'Reported. The project manager has been notified.');
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
        <FormField label="Type" htmlFor="issue-type" error={errors.type?.message} required>
          <Select id="issue-type" options={types.data ?? []} invalid={!!errors.type} {...register('type')} />
        </FormField>
        <FormField label="Severity" htmlFor="issue-severity" error={errors.severity?.message} required>
          <Select id="issue-severity" options={severities.data ?? []} invalid={!!errors.severity} {...register('severity')} />
        </FormField>
      </div>
      <FormField label="Title" htmlFor="issue-title" error={errors.title?.message} required>
        <Input id="issue-title" invalid={!!errors.title} {...register('title')} />
      </FormField>
      <FormField label="What happened, or what could happen" htmlFor="issue-description" error={errors.description?.message} required>
        <Textarea id="issue-description" rows={4} maxLength={2000} invalid={!!errors.description} {...register('description')} />
      </FormField>
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField label="Assigned to" htmlFor="issue-assignee" error={errors.assignedToId?.message}>
          <Select id="issue-assignee" options={members} placeholder="Nobody yet" {...register('assignedToId')} />
        </FormField>
        <FormField label="Deal with by" htmlFor="issue-due" error={errors.dueDate?.message}>
          <Input id="issue-due" type="date" {...register('dueDate')} />
        </FormField>
      </div>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="secondary" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" loading={isSubmitting}>
          {issue ? 'Save' : 'Report'}
        </Button>
      </div>
    </form>
  );
}

function IssueDetails({ issue, perms, onEdit, onClose }: { issue: Issue; perms: ProjectPermissions; onEdit: () => void; onClose: () => void }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [target, setTarget] = useState('');
  const [resolution, setResolution] = useState(issue.resolution ?? '');
  const [error, setError] = useState<string | undefined>();
  const needsResolution = target === 'RESOLVED' || target === 'CLOSED';

  const change = useMutation({
    mutationFn: () => issuesApi.changeStatus(issue.id, target, needsResolution ? resolution.trim() : null),
    onSuccess: async (updated) => {
      await queryClient.invalidateQueries({ queryKey: projectKeys.issues(issue.projectId) });
      await queryClient.invalidateQueries({ queryKey: projectKeys.detail(issue.projectId) });
      toast.success(`Moved to ${humanize(updated.status).toLowerCase()}.`);
      onClose();
    },
    onError: (err) => {
      const apiError = toApiError(err);
      setError(apiError.fieldErrors.resolution ?? apiError.message);
    },
  });

  const submit = () => {
    setError(undefined);
    if (needsResolution && resolution.trim().length < 5) {
      setError(CLIENT_MESSAGES.resolution);
      return;
    }
    change.mutate();
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-wrap gap-2">
        <StatusBadge status={issue.type} />
        <StatusBadge status={issue.severity} label={`${humanize(issue.severity)} severity`} />
        <StatusBadge status={issue.status} />
      </div>
      <p className="whitespace-pre-line text-sm text-ink">{issue.description}</p>
      <DetailList
        items={[
          { label: 'Reported by', value: `${issue.reportedByName}, ${formatDateTime(issue.createdAt)}` },
          { label: 'Assigned to', value: issue.assignedToName ?? '-' },
          { label: 'Deal with by', value: formatDate(issue.dueDate) },
          ...(issue.resolution ? [{ label: 'Resolution', value: issue.resolution }] : []),
        ]}
      />
      {perms.canChangeIssueStatus(issue) && issue.nextStatuses.length > 0 && (
        <div className="space-y-3 rounded-lg bg-surface p-4">
          <FormField label="Move to" htmlFor="issue-next">
            <Select
              id="issue-next"
              options={issue.nextStatuses.map((s) => ({ value: s, label: humanize(s) }))}
              placeholder="Choose a status"
              value={target}
              onChange={(e) => setTarget(e.target.value)}
            />
          </FormField>
          {needsResolution && (
            <FormField label="Resolution" htmlFor="issue-resolution" error={error} required>
              <Textarea id="issue-resolution" rows={3} maxLength={2000} invalid={!!error} value={resolution} onChange={(e) => setResolution(e.target.value)} />
            </FormField>
          )}
          {!needsResolution && error && <p className="text-sm text-red-600">{error}</p>}
          <Button size="sm" disabled={!target} loading={change.isPending} onClick={submit}>
            Update status
          </Button>
        </div>
      )}
      {perms.canEditIssue(issue) && (
        <Button variant="secondary" size="sm" onClick={onEdit}>
          Edit details
        </Button>
      )}
    </div>
  );
}

/** Issues (problems now) and risks (problems that may happen) for a project. */
export function IssuesTab({ project, perms }: { project: ProjectDetail; perms: ProjectPermissions }) {
  const table = useTableState('createdAt', 'desc');
  const [filters, setFilters] = useState<IssueFilters>({});
  const [form, setForm] = useState<{ issue: Issue | null } | null>(null);
  const [open, setOpen] = useState<Issue | null>(null);
  const types = useEnumLookup('issue-types');
  const severities = useEnumLookup('issue-severities');
  const statuses = useEnumLookup('issue-statuses');
  const members = useQuery({ queryKey: projectKeys.members(project.id), queryFn: () => projectsApi.members(project.id) });
  const memberOptions = useMemo(
    () => (members.data ?? []).map((m) => ({ value: String(m.userId), label: `${m.fullName} (${m.roleDisplayName})` })),
    [members.data],
  );

  const issues = useQuery({
    queryKey: [...projectKeys.issues(project.id), table.params, filters],
    queryFn: () => issuesApi.list(project.id, table.params, filters),
    placeholderData: keepPreviousData,
  });

  const setFilter = (key: keyof IssueFilters, value: string) => {
    setFilters((current) => ({ ...current, [key]: value || undefined }));
    table.resetPage();
  };

  const columns: Column<Issue>[] = [
    {
      key: 'title',
      header: 'Title',
      sortKey: 'title',
      render: (i) => (
        <button type="button" onClick={() => setOpen(i)} className="text-left font-medium text-ink hover:text-primary">
          {i.title}
        </button>
      ),
    },
    { key: 'type', header: 'Type', render: (i) => <StatusBadge status={i.type} /> },
    { key: 'severity', header: 'Severity', sortKey: 'severity', render: (i) => <StatusBadge status={i.severity} /> },
    { key: 'status', header: 'Status', sortKey: 'status', render: (i) => <StatusBadge status={i.status} /> },
    { key: 'assigned', header: 'Assigned to', render: (i) => i.assignedToName ?? '-' },
    { key: 'reported', header: 'Reported', sortKey: 'createdAt', className: 'whitespace-nowrap', render: (i) => `${formatDate(i.createdAt)}, ${i.reportedByName}` },
  ];

  return (
    <div className="space-y-4">
      {perms.canReportIssues && (
        <div className="flex justify-end">
          <Button onClick={() => setForm({ issue: null })}>
            <Plus className="h-4 w-4" aria-hidden />
            Report issue or risk
          </Button>
        </div>
      )}
      <DataTable
        columns={columns}
        page={issues.data}
        rowKey={(i) => i.id}
        loading={issues.isFetching}
        error={issues.isError ? toApiError(issues.error).message : null}
        emptyMessage="No issues or risks recorded."
        sort={table.sort}
        direction={table.direction}
        onSort={table.toggleSort}
        onPageChange={table.setPage}
        toolbar={
          <>
            <SearchInput value={table.searchInput} onChange={table.changeSearch} placeholder="Search titles" />
            <div className="flex flex-wrap gap-2 sm:ml-auto">
              <Select aria-label="Filter by type" options={types.data ?? []} placeholder="Issues and risks" value={filters.type ?? ''} onChange={(e) => setFilter('type', e.target.value)} />
              <Select aria-label="Filter by severity" options={severities.data ?? []} placeholder="Any severity" value={filters.severity ?? ''} onChange={(e) => setFilter('severity', e.target.value)} />
              <Select aria-label="Filter by status" options={statuses.data ?? []} placeholder="Any status" value={filters.status ?? ''} onChange={(e) => setFilter('status', e.target.value)} />
            </div>
          </>
        }
      />

      <Modal open={form !== null} title={form?.issue ? 'Edit issue or risk' : 'Report an issue or risk'} onClose={() => setForm(null)} size="lg">
        {form && (
          <IssueForm
            projectId={project.id}
            members={memberOptions}
            issue={form.issue}
            onDone={() => {
              setForm(null);
              setOpen(null);
            }}
          />
        )}
      </Modal>
      <Modal open={open !== null} title={open?.title ?? ''} onClose={() => setOpen(null)} size="lg">
        {open && <IssueDetails issue={open} perms={perms} onEdit={() => setForm({ issue: open })} onClose={() => setOpen(null)} />}
      </Modal>
    </div>
  );
}
