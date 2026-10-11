package com.sitemaster.audit;

// PATTERN: Observer

import com.sitemaster.common.util.MessageResolver;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowObserver;
import org.springframework.stereotype.Component;

/**
 * Observer pattern - a CONCRETE OBSERVER. Writes one audit entry for every
 * workflow event, using the event.TYPE.audit message.
 * <p>
 * OOP concept: Polymorphism - one of several WorkflowObserver implementations.
 */
@Component
public class AuditLogObserver implements WorkflowObserver {

    private static final int MAX_DESCRIPTION = 1000;

    private final AuditLogRepository repository;
    private final MessageResolver messages;

    public AuditLogObserver(AuditLogRepository repository, MessageResolver messages) {
        this.repository = repository;
        this.messages = messages;
    }

    @Override
    public void onEvent(WorkflowEvent event) {
        String description = messages.get(event.type().auditKey(), event.messageArguments());
        if (description.length() > MAX_DESCRIPTION) {
            description = description.substring(0, MAX_DESCRIPTION);
        }
        repository.save(new AuditLog(event.actorId(), event.actorName(), event.type().name(),
                event.type().getModule(), event.type().getEntityType(), event.entityId(), description));
    }
}
