package com.sitemaster.notification;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.notification.dto.NotificationResponse;
import com.sitemaster.security.CurrentUserProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Implementation of {@link NotificationService}.
 * <p>
 * OOP concept: Abstraction - implements NotificationService.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Map<String, String> SORTS = Map.of("createdAt", "createdAt");

    private final NotificationRepository repository;
    private final CurrentUserProvider currentUser;
    private final Clock clock;

    public NotificationServiceImpl(NotificationRepository repository, CurrentUserProvider currentUser, Clock clock) {
        this.repository = repository;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> listMine(boolean unreadOnly, PageQuery pageQuery) {
        Long userId = currentUser.requireId();
        Pageable pageable = pageQuery.toPageable(SORTS, "createdAt", Sort.Direction.DESC);
        return PageResponse.from(unreadOnly
                        ? repository.findByRecipientIdAndReadFalse(userId, pageable)
                        : repository.findByRecipientId(userId, pageable),
                NotificationServiceImpl::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMyUnread() {
        return repository.countByRecipientIdAndReadFalse(currentUser.requireId());
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long notificationId) {
        Notification notification = repository.findByIdAndRecipientId(notificationId, currentUser.requireId())
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Notification"));
        notification.markRead(LocalDateTime.now(clock));
        return toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllRead() {
        return repository.markAllRead(currentUser.requireId(), LocalDateTime.now(clock));
    }

    private static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getEventType(), n.getTitle(), n.getMessage(),
                n.getLink(), n.isRead(), n.getCreatedAt());
    }
}
