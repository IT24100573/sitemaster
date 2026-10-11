export interface Issue {
  id: number;
  projectId: number;
  projectName: string;
  type: 'ISSUE' | 'RISK';
  title: string;
  description: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
  nextStatuses: string[];
  reportedById: number;
  reportedByName: string;
  assignedToId: number | null;
  assignedToName: string | null;
  dueDate: string | null;
  resolution: string | null;
  resolvedAt: string | null;
  createdAt: string;
}

export interface IssuePayload {
  type: string;
  title: string;
  description: string;
  severity: string;
  assignedToId: number | null;
  dueDate: string | null;
}

export interface IssueFilters {
  type?: string;
  status?: string;
  severity?: string;
}
