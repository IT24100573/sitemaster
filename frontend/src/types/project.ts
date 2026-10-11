export type ProjectStatus = 'PLANNED' | 'ACTIVE' | 'DELAYED' | 'COMPLETED' | 'ARCHIVED';

export interface ProjectSummary {
  id: number;
  name: string;
  clientName: string;
  managerName: string;
  location: string;
  status: ProjectStatus;
  progress: number;
  startDate: string;
  endDate: string;
  budget: number;
  hasCover: boolean;
}

export interface ProjectDetail {
  id: number;
  name: string;
  description: string | null;
  clientId: number;
  clientName: string;
  managerId: number;
  managerName: string;
  location: string;
  latitude: number;
  longitude: number;
  startDate: string;
  endDate: string;
  completedDate: string | null;
  budget: number;
  allocatedBudget: number;
  status: ProjectStatus;
  nextStatuses: ProjectStatus[];
  progress: number;
  hasCover: boolean;
  memberCount: number;
  taskCount: number;
  openIssueCount: number;
  canManage: boolean;
  isMember: boolean;
}

export interface ProjectPayload {
  name: string;
  description: string | null;
  clientId: number;
  managerId: number;
  location: string;
  latitude: string;
  longitude: string;
  startDate: string;
  endDate: string;
  budget: string;
}

export interface TeamMember {
  userId: number;
  fullName: string;
  email: string;
  phone: string | null;
  role: string;
  roleDisplayName: string;
  hasPhoto: boolean;
  manager: boolean;
  addedAt: string;
}

export interface Phase {
  id: number;
  name: string;
  description: string | null;
  startDate: string;
  endDate: string;
  budget: number;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  sortOrder: number;
  taskCount: number;
  openTaskCount: number;
}

export interface PhasePayload {
  name: string;
  description: string | null;
  startDate: string;
  endDate: string;
  budget: string;
  sortOrder: number;
}

export interface Milestone {
  id: number;
  name: string;
  dueDate: string;
  phaseId: number | null;
  phaseName: string | null;
  completed: boolean;
  completedDate: string | null;
  overdue: boolean;
}

export interface MilestonePayload {
  name: string;
  dueDate: string;
  phaseId: number | null;
}
