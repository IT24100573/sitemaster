package com.sitemaster.notification;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * An in-app message for one user.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (content is
 * immutable; the only state change is {@link #markRead}).
 */
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false, updatable = false)
    private User recipient;

    @Column(name = "event_type", nullable = false, length = 60, updatable = false)
    private String eventType;

    @Column(name = "title", nullable = false, length = 150, updatable = false)
    private String title;

    @Column(name = "message", nullable = false, length = 500, updatable = false)
    private String message;

    @Column(name = "link", length = 255, updatable = false)
    private String link;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected Notification() {
        // required by JPA
    }

    public Notification(User recipient, String eventType, String title, String message, String link) {
        this.recipient = recipient;
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.link = link;
    }

    public void markRead(LocalDateTime now) {
        if (!read) {
            read = true;
            readAt = now;
        }
    }

    public boolean belongsTo(Long userId) {
        return recipient.getId().equals(userId);
    }

    public User getRecipient() {
        return recipient;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getLink() {
        return link;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }
}
