package com.sitemaster.security;

import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Gives services the signed-in user. Services use this for ownership checks
 * instead of trusting ids sent by the client.
 * <p>
 * OOP concept: Abstraction - services do not touch the SecurityContext directly.
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public SiteMasterUserPrincipal requirePrincipal() {
        return SecurityUtils.currentPrincipal()
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Not signed in"));
    }

    public Long requireId() {
        return requirePrincipal().getUserId();
    }

    /** The signed-in user's entity, loaded fresh from the database. */
    public User requireUser() {
        return userRepository.findById(requireId())
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "User"));
    }
}
