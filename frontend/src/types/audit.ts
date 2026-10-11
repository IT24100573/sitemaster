export interface AuditLogEntry {
  id: number;
  createdAt: string;
  actorId: number | null;
  actorName: string;
  action: string;
  actionLabel: string;
  module: string;
  entityType: string | null;
  entityId: number | null;
  description: string;
}

export interface AuditLogFilters {
  userId?: string;
  module?: string;
  from?: string;
  to?: string;
}
