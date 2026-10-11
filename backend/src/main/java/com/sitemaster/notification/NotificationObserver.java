package com.sitemaster.notification;

// PATTERN: Observer

import com.sitemaster.common.util.MessageResolver;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowObserver;
import org.springframework.stereotype.Component;

/**
 * Observer pattern - a CONCRETE OBSERVER. Creates an in-app notification for
 * each recipient of a workflow event (the "next person" in the hand-off).
 * <p>
 * OOP concept: Polymorphism - one of several WorkflowObserver implementations.
 */
@Component
public class NotificationObserver implements WorkflowObserver {

    private static final int MAX_TITLE = 150;
    private static final int MAX_MESSAGE = 500;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final MessageResolver messages;

    public NotificationObserver(NotificationRepository notificationRepository,
                                UserRepository userRepository,
                                MessageResolver messages) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messages = messages;
    }

    @Override
    public void onEvent(WorkflowEvent event) {
        if (event.recipientIds().isEmpty()) {
            return;
        }
        Object[] args = event.messageArguments();
        String title = truncate(messages.get(event.type().titleKey(), args), MAX_TITLE);
        String message = truncate(messages.get(event.type().messageKey(), args), MAX_MESSAGE);
        userRepository.findAllById(event.recipientIds()).forEach(recipient ->
                notificationRepository.save(new Notification(recipient, event.type().name(), title, message, event.link())));
    }

    private static String truncate(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }
}
