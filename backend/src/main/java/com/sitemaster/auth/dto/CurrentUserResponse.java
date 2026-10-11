package com.sitemaster.auth.dto;

import java.util.List;

/**
 * Response of GET /api/auth/me and POST /api/auth/login.
 * <p>
 * OOP concept: Encapsulation - an immutable DTO; the User entity is never exposed.
 *
 * @param id              user id
 * @param fullName        display name
 * @param email           sign-in email
 * @param role            role enum name, e.g. SITE_ENGINEER
 * @param roleDisplayName human-readable role, e.g. Site Engineer
 * @param rolePath        URL segment for the role, e.g. site
 * @param permissions     MODULE:ACTION authorities granted to the role
 */
public record CurrentUserResponse(Long id,
                                  String fullName,
                                  String email,
                                  String role,
                                  String roleDisplayName,
                                  String rolePath,
                                  List<String> permissions) {

    public CurrentUserResponse {
        permissions = permissions == null ? List.of() : List.copyOf(permissions);
    }
}
