package com.sitemaster.notification;

import com.sitemaster.common.dto.MessageResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.util.MessageResolver;
import com.sitemaster.notification.dto.NotificationResponse;
import com.sitemaster.notification.dto.UnreadCountResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Notifications of the signed-in user (any role). */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final MessageResolver messages;

    public NotificationController(NotificationService notificationService, MessageResolver messages) {
        this.notificationService = notificationService;
        this.messages = messages;
    }

    @GetMapping
    public PageResponse<NotificationResponse> list(@RequestParam(defaultValue = "false") boolean unreadOnly,
                                                   @Valid @ModelAttribute PageQuery pageQuery) {
        return notificationService.listMine(unreadOnly, pageQuery);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount() {
        return new UnreadCountResponse(notificationService.countMyUnread());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id) {
        return notificationService.markRead(id);
    }

    @PatchMapping("/read-all")
    public MessageResponse markAllRead() {
        int updated = notificationService.markAllRead();
        return new MessageResponse(messages.get("notification.allRead", updated));
    }
}
