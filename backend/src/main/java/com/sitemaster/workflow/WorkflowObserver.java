package com.sitemaster.workflow;

// PATTERN: Observer

/**
 * Observer pattern - the OBSERVER role. Anything that must react to workflow
 * hand-offs implements this interface and is registered with the
 * WorkflowEventPublisher; the publisher never knows the concrete classes.
 * <p>
 * Implementations: NotificationObserver (in-app notifications) and
 * AuditLogObserver (audit trail).
 * <p>
 * OOP concepts: Abstraction and Polymorphism.
 */
public interface WorkflowObserver {

    void onEvent(WorkflowEvent event);
}
