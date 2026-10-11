/** React Query keys shared by the project page and its tabs. */
export const projectKeys = {
  detail: (id: number) => ['projects', 'detail', id] as const,
  members: (id: number) => ['projects', id, 'members'] as const,
  candidates: (id: number) => ['projects', id, 'candidates'] as const,
  phases: (id: number) => ['projects', id, 'phases'] as const,
  milestones: (id: number) => ['projects', id, 'milestones'] as const,
  tasks: (id: number) => ['projects', id, 'tasks'] as const,
  documents: (id: number) => ['projects', id, 'documents'] as const,
  issues: (id: number) => ['projects', id, 'issues'] as const,
};
