package com.sitemaster.seed;

import com.sitemaster.security.PrincipalFactory;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Runs a piece of demo seeding as a given user, so services apply exactly the
 * same permission and ownership rules as for a real request.
 */
@Component
class RunAs {

    private final UserRepository userRepository;
    private final PrincipalFactory principalFactory;

    RunAs(UserRepository userRepository, PrincipalFactory principalFactory) {
        this.userRepository = userRepository;
        this.principalFactory = principalFactory;
    }

    <T> T user(String email, Supplier<T> work) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Demo user " + email + " is missing"));
        SiteMasterUserPrincipal principal = principalFactory.create(user);
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        try {
            return work.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }

    void run(String email, Runnable work) {
        user(email, () -> {
            work.run();
            return null;
        });
    }

    /** Public, unauthenticated actions such as a supplier application. */
    <T> T anonymous(Supplier<T> work) {
        SecurityContext previous = SecurityContextHolder.getContext();
        SecurityContextHolder.setContext(SecurityContextHolder.createEmptyContext());
        try {
            return work.get();
        } finally {
            SecurityContextHolder.setContext(previous);
        }
    }
}
