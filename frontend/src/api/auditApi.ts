import { api } from './client';
import { cleanParams } from '../lib/params';
import type { AuditLogEntry, AuditLogFilters } from '../types/audit';
import type { PageParams, PageResponse } from '../types/common';

export const auditApi = {
  list: async (page: PageParams, filters: AuditLogFilters) =>
    (await api.get<PageResponse<AuditLogEntry>>('/admin/audit-logs', { params: cleanParams({ ...page, ...filters }) }))
      .data,
};
