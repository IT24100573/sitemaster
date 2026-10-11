import { api } from './client';
import { cleanParams } from '../lib/params';
import type { MessageResponse } from '../types/api';
import type { PageParams, PageResponse } from '../types/common';
import type { NotificationItem } from '../types/notification';

export const notificationsApi = {
  list: async (page: PageParams, unreadOnly: boolean) =>
    (
      await api.get<PageResponse<NotificationItem>>('/notifications', {
        params: cleanParams({ ...page, unreadOnly }),
      })
    ).data,
  unreadCount: async () => (await api.get<{ count: number }>('/notifications/unread-count')).data.count,
  markRead: async (id: number) => (await api.patch<NotificationItem>(`/notifications/${id}/read`)).data,
  markAllRead: async () => (await api.patch<MessageResponse>('/notifications/read-all')).data,
};
