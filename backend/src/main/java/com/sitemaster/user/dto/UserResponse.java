package com.sitemaster.user.dto;

import java.time.LocalDateTime;

/**
 * A user as shown in User Management. Never includes the password hash.
 *
 * @param id              user id
 * @param fullName        display name
 * @param email           sign-in email
 * @param phone           phone number (may be null)
 * @param role            role enum name
 * @param roleDisplayName readable role
 * @param active          false when deactivated
 * @param locked          true while locked after failed sign-ins
 * @param hasPhoto        whether a profile photo exists
 * @param lastLoginAt     last successful sign-in
 * @param createdAt       when the account was created
 */
public record UserResponse(Long id,
                           String fullName,
                           String email,
                           String phone,
                           String role,
                           String roleDisplayName,
                           boolean active,
                           boolean locked,
                           boolean hasPhoto,
                           LocalDateTime lastLoginAt,
                           LocalDateTime createdAt) {
}
