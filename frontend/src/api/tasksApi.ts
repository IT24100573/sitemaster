import { api } from './client';
import { cleanParams } from '../lib/params';
import type { PageParams, PageResponse } from '../types/common';
import type { MyWork, SubTask, SubTaskPayload, Task, TaskFilters, TaskPayload, TaskStatus } from '../types/task';

export const tasksApi = {
  list: async (projectId: number, page: PageParams, filters: TaskFilters) =>
    (
      await api.get<PageResponse<Task>>(`/projects/${projectId}/tasks`, {
        params: cleanParams({ ...page, ...filters }),
      })
    ).data,
  create: async (projectId: number, payload: TaskPayload) =>
    (await api.post<Task>(`/projects/${projectId}/tasks`, payload)).data,
  update: async (taskId: number, payload: TaskPayload) => (await api.put<Task>(`/tasks/${taskId}`, payload)).data,
  progress: async (taskId: number, status: TaskStatus, progress: number) =>
    (await api.patch<Task>(`/tasks/${taskId}/progress`, { status, progress })).data,
  remove: async (taskId: number) => {
    await api.delete(`/tasks/${taskId}`);
  },
  addSubTask: async (taskId: number, payload: SubTaskPayload) =>
    (await api.post<Task>(`/tasks/${taskId}/subtasks`, payload)).data,
  updateSubTask: async (subTaskId: number, payload: SubTaskPayload) =>
    (await api.put<Task>(`/subtasks/${subTaskId}`, payload)).data,
  subTaskStatus: async (subTaskId: number, status: TaskStatus) =>
    (await api.patch<SubTask>(`/subtasks/${subTaskId}/status`, { status })).data,
  removeSubTask: async (subTaskId: number) => (await api.delete<Task>(`/subtasks/${subTaskId}`)).data,
  mine: async () => (await api.get<MyWork>('/tasks/mine')).data,
};
