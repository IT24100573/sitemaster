import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { UserMinus, UserPlus } from 'lucide-react';
import { projectsApi } from '../../../api/projectsApi';
import { useToast } from '../../../context/ToastContext';
import { toApiError } from '../../../lib/apiError';
import { formatDate } from '../../../lib/format';
import { Alert } from '../../../components/ui/Alert';
import { Avatar } from '../../../components/ui/Avatar';
import { Button } from '../../../components/ui/Button';
import { Card } from '../../../components/ui/Card';
import { ConfirmDialog } from '../../../components/ui/ConfirmDialog';
import { Select } from '../../../components/ui/Select';
import { Spinner } from '../../../components/ui/Spinner';
import { projectKeys } from '../queryKeys';
import type { ProjectPermissions } from '../useProjectPermissions';
import type { ProjectDetail, TeamMember } from '../../../types/project';

/** Team list; the PM adds site engineers and workers, a site engineer adds workers. */
export function TeamTab({ project, perms }: { project: ProjectDetail; perms: ProjectPermissions }) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [userId, setUserId] = useState('');
  const [removing, setRemoving] = useState<TeamMember | null>(null);

  const members = useQuery({ queryKey: projectKeys.members(project.id), queryFn: () => projectsApi.members(project.id) });
  const candidates = useQuery({
    queryKey: projectKeys.candidates(project.id),
    queryFn: () => projectsApi.candidates(project.id),
    enabled: perms.canManageTeam,
  });

  const refresh = async () => {
    await queryClient.invalidateQueries({ queryKey: ['projects', project.id] });
    await queryClient.invalidateQueries({ queryKey: projectKeys.detail(project.id) });
  };

  const add = useMutation({
    mutationFn: () => projectsApi.addMember(project.id, Number(userId)),
    onSuccess: async (member) => {
      setUserId('');
      await refresh();
      toast.success(`${member.fullName} was added to the team.`);
    },
    onError: (error) => toast.error(toApiError(error).message),
  });

  const remove = useMutation({
    mutationFn: (member: TeamMember) => projectsApi.removeMember(project.id, member.userId),
    onSuccess: async (_data, member) => {
      setRemoving(null);
      await refresh();
      toast.success(`${member.fullName} was removed from the team.`);
    },
    onError: (error) => {
      setRemoving(null);
      toast.error(toApiError(error).message);
    },
  });

  const canRemove = (m: TeamMember) =>
    perms.canManageTeam && !m.manager && (perms.canManage ? m.role !== 'PROJECT_MANAGER' : m.role === 'WORKER');

  return (
    <div className="space-y-6">
      {perms.canManageTeam && (
        <Card title="Add to team" description={perms.canManage ? 'Site engineers and workers.' : 'Workers for this site.'}>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Select
              aria-label="Person to add"
              options={candidates.data ?? []}
              placeholder={candidates.data?.length === 0 ? 'Everyone eligible is already on the team' : 'Choose a person'}
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
            />
            <Button disabled={!userId} loading={add.isPending} onClick={() => add.mutate()} className="shrink-0">
              <UserPlus className="h-4 w-4" aria-hidden />
              Add
            </Button>
          </div>
        </Card>
      )}

      <Card title="Team members">
        {members.isLoading ? (
          <Spinner />
        ) : members.isError ? (
          <Alert tone="error">{toApiError(members.error).message}</Alert>
        ) : (
          <ul className="divide-y divide-line">
            {members.data?.map((m) => (
              <li key={m.userId} className="flex items-center gap-3 py-3">
                <Avatar userId={m.userId} name={m.fullName} hasPhoto={m.hasPhoto} />
                <div className="min-w-0 flex-1">
                  <p className="truncate text-sm font-medium text-ink">
                    {m.fullName}
                    {m.manager && <span className="ml-2 text-xs font-normal text-primary-dark">Project manager</span>}
                  </p>
                  <p className="truncate text-xs text-muted">
                    {m.roleDisplayName}, {m.email}
                    {m.phone ? `, ${m.phone}` : ''}
                  </p>
                </div>
                <span className="hidden text-xs text-muted sm:block">Since {formatDate(m.addedAt)}</span>
                {canRemove(m) && (
                  <button
                    type="button"
                    onClick={() => setRemoving(m)}
                    className="rounded-md p-2 text-red-600 hover:bg-surface"
                    aria-label={`Remove ${m.fullName}`}
                    title="Remove from team"
                  >
                    <UserMinus className="h-4 w-4" aria-hidden />
                  </button>
                )}
              </li>
            ))}
          </ul>
        )}
      </Card>

      <ConfirmDialog
        open={removing !== null}
        title={`Remove ${removing?.fullName ?? ''} from the team?`}
        message="They will no longer see this project. People with open tasks cannot be removed."
        confirmLabel="Remove"
        tone="danger"
        loading={remove.isPending}
        onConfirm={() => removing && remove.mutate(removing)}
        onCancel={() => setRemoving(null)}
      />
    </div>
  );
}
