package com.sitemaster.notification.dto;

import java.time.LocalDateTime;

/**
 * A notification as shown in the bell menu and on the Notifications page.
 *
 * @param id        notification id
 * @param eventType what triggered it
 * @param title     short heading
 * @param message   full text
 * @param link      role-relative page to open, e.g. "profile" (may be null)
 * @param read      whether the user has opened it
 * @param createdAt when it was created
 */
public record NotificationResponse(Long id,
                                   String eventType,
                                   String title,
                                   String message,
                                   String link,
                                   boolean read,
                                   LocalDateTime createdAt) {
}
