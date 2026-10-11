export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export interface SubTask {
  id: number;
  taskId: number;
  taskTitle: string;
  projectId: number;
  projectName: string;
  title: string;
  assigneeId: number | null;
  assigneeName: string | null;
  status: TaskStatus;
  dueDate: string | null;
  completedAt: string | null;
}

export interface Task {
  id: number;
  projectId: number;
  projectName: string;
  phaseId: number;
  phaseName: string;
  title: string;
  description: string | null;
  assigneeId: number | null;
  assigneeName: string | null;
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';
  dueDate: string;
  status: TaskStatus;
  progress: number;
  completedAt: string | null;
  overdue: boolean;
  subTasks: SubTask[];
}

export interface TaskPayload {
  phaseId: number;
  title: string;
  description: string | null;
  assigneeId: number | null;
  priority: string;
  dueDate: string;
}

export interface SubTaskPayload {
  title: string;
  assigneeId: number | null;
  dueDate: string | null;
}

export interface MyWork {
  tasks: Task[];
  subTasks: SubTask[];
}

export interface TaskFilters {
  phaseId?: string;
  status?: string;
  assigneeId?: string;
}
