package com.sitemaster.audit.dto;

import java.time.LocalDateTime;

/**
 * One audit entry as shown on the Audit Log page.
 *
 * @param id          entry id
 * @param createdAt   when it happened
 * @param actorId     user who did it (null for the system)
 * @param actorName   name at the time
 * @param action      event type, e.g. USER_CREATED
 * @param actionLabel readable action, e.g. User created
 * @param module      module, e.g. USER
 * @param entityType  affected entity type
 * @param entityId    affected record id
 * @param description full sentence describing the action
 */
public record AuditLogResponse(Long id,
                               LocalDateTime createdAt,
                               Long actorId,
                               String actorName,
                               String action,
                               String actionLabel,
                               String module,
                               String entityType,
                               Long entityId,
                               String description) {
}
