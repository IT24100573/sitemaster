package com.sitemaster.workflow;

// PATTERN: Observer

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer pattern - the SUBJECT role. Services call {@link #publish} at every
 * workflow hand-off; every subscribed {@link WorkflowObserver} is notified in
 * turn. Spring passes in all WorkflowObserver beans at start-up, and
 * {@link #subscribe}/{@link #unsubscribe} show that observers can also be
 * attached and detached at runtime.
 * <p>
 * Observers run synchronously inside the caller's transaction, so the action,
 * its notification and its audit entry are saved together or not at all.
 */
@Component
public class WorkflowEventPublisher {

    private final List<WorkflowObserver> observers = new CopyOnWriteArrayList<>();

    public WorkflowEventPublisher(List<WorkflowObserver> initialObservers) {
        observers.addAll(initialObservers);
    }

    public void subscribe(WorkflowObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void unsubscribe(WorkflowObserver observer) {
        observers.remove(observer);
    }

    public void publish(WorkflowEvent event) {
        for (WorkflowObserver observer : observers) {
            observer.onEvent(event);
        }
    }

    /** Number of subscribed observers (used by tests). */
    public int observerCount() {
        return observers.size();
    }
}
