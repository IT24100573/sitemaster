package com.sitemaster.auth;

import com.sitemaster.auth.dto.CurrentUserResponse;
import com.sitemaster.security.SiteMasterUserPrincipal;

/**
 * Maps the session principal to its response DTO.
 * <p>
 * OOP concept: Encapsulation - controllers never build DTOs by hand.
 */
public final class AuthMapper {

    private AuthMapper() {
        // utility class
    }

    public static CurrentUserResponse toCurrentUser(SiteMasterUserPrincipal principal) {
        return new CurrentUserResponse(
                principal.getUserId(),
                principal.getFullName(),
                principal.getEmail(),
                principal.getRole().name(),
                principal.getRole().getDisplayName(),
                principal.getRole().getRolePath(),
                principal.getPermissions().stream().sorted().toList());
    }
}
