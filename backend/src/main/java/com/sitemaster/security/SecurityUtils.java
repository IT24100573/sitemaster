package com.sitemaster.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Static helpers to read the signed-in user from the security context.
 * <p>
 * OOP concept: Encapsulation - one place knows how the principal is stored.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // utility class
    }

    public static Optional<SiteMasterUserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof SiteMasterUserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Optional<Long> currentUserId() {
        return currentPrincipal().map(SiteMasterUserPrincipal::getUserId);
    }
}
