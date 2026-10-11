import { useAuth, useCurrentUser } from '../../context/AuthContext';
import type { ProjectDetail } from '../../types/project';
import type { SubTask, Task } from '../../types/task';
import type { Issue } from '../../types/issue';

/**
 * What the current user may do on a project page. These flags only decide which
 * buttons appear; the API applies the same rules and is authoritative.
 */
export function useProjectPermissions(project: ProjectDetail) {
  const user = useCurrentUser();
  const { hasPermission } = useAuth();
  const archived = project.status === 'ARCHIVED';
  const manager = project.canManage;
  const engineerOnTeam = user.role === 'SITE_ENGINEER' && project.isMember && !archived;
  const onTeam = (project.isMember || manager) && !archived;

  return {
    userId: user.id,
    canManage: manager,
    canManageTeam: manager || engineerOnTeam,
    canCreateTasks: manager && hasPermission('TASK:CREATE'),
    canDeleteTasks: manager && hasPermission('TASK:DELETE'),
    canPlanSubTasks: hasPermission('TASK:CREATE') && (manager || engineerOnTeam),
    canUpdateTask: (task: Task) =>
      !archived && hasPermission('TASK:EDIT') && (manager || engineerOnTeam || task.assigneeId === user.id),
    canUpdateSubTask: (sub: SubTask, task: Task) =>
      !archived &&
      hasPermission('TASK:EDIT') &&
      (manager || engineerOnTeam || sub.assigneeId === user.id || task.assigneeId === user.id),
    canViewTasks: hasPermission('TASK:VIEW'),
    canViewDocuments: hasPermission('DOCUMENT:VIEW'),
    canUploadDocuments: manager && hasPermission('DOCUMENT:CREATE'),
    canVersionDocuments: manager && hasPermission('DOCUMENT:EDIT'),
    canDeleteDocuments: manager && hasPermission('DOCUMENT:DELETE'),
    canViewIssues: hasPermission('ISSUE:VIEW'),
    canReportIssues: onTeam && hasPermission('ISSUE:CREATE'),
    canEditIssue: (issue: Issue) =>
      !archived && hasPermission('ISSUE:EDIT') && issue.status !== 'CLOSED' && (manager || issue.reportedById === user.id),
    canChangeIssueStatus: (issue: Issue) =>
      !archived &&
      hasPermission('ISSUE:EDIT') &&
      (manager || issue.reportedById === user.id || issue.assignedToId === user.id),
    canSeeBudget: manager || user.role === 'ACCOUNTANT' || user.role === 'ADMIN',
  };
}

export type ProjectPermissions = ReturnType<typeof useProjectPermissions>;
