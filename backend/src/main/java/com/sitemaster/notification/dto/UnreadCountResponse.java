package com.sitemaster.notification.dto;

/**
 * Unread notifications for the bell badge.
 *
 * @param count number of unread notifications
 */
public record UnreadCountResponse(long count) {
}
