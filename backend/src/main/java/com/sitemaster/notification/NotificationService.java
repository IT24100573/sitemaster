package com.sitemaster.notification;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.notification.dto.NotificationResponse;

/**
 * The signed-in user's notifications. Every method is scoped to the current user,
 * so nobody can read or change another user's notifications by changing an id.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface NotificationService {

    PageResponse<NotificationResponse> listMine(boolean unreadOnly, PageQuery pageQuery);

    long countMyUnread();

    NotificationResponse markRead(Long notificationId);

    int markAllRead();
}
