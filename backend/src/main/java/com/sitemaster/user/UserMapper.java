package com.sitemaster.user;

import com.sitemaster.user.dto.UserResponse;

import java.time.LocalDateTime;

/**
 * Maps User entities to response DTOs.
 * <p>
 * OOP concept: Encapsulation - entities never leave the service layer.
 */
public final class UserMapper {

    private UserMapper() {
        // utility class
    }

    public static UserResponse toResponse(User user, LocalDateTime now) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole().getName().name(), user.getRole().getDisplayName(), user.isActive(),
                user.isLockedAt(now), user.hasProfilePhoto(), user.getLastLoginAt(), user.getCreatedAt());
    }
}
