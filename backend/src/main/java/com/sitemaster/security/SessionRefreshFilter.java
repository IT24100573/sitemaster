package com.sitemaster.security;

import com.sitemaster.user.UserRepository;
import com.sitemaster.user.UserSecurityState;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Keeps sessions in step with the database on every request:
 * <ul>
 *   <li>a deactivated (or deleted) user's session is ended immediately with 401;</li>
 *   <li>when the user's security version changed (role changed, permissions of
 *       their role edited, name changed), the principal in the session is rebuilt.</li>
 * </ul>
 * It costs one small indexed query per authenticated request.
 * Created inside SecurityConfig (not a @Component) so it only runs in the
 * security filter chain.
 */
public class SessionRefreshFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final PrincipalFactory principalFactory;
    private final SecurityContextRepository securityContextRepository;
    private final ErrorResponseWriter errorWriter;

    public SessionRefreshFilter(UserRepository userRepository, PrincipalFactory principalFactory,
                                SecurityContextRepository securityContextRepository, ErrorResponseWriter errorWriter) {
        this.userRepository = userRepository;
        this.principalFactory = principalFactory;
        this.securityContextRepository = securityContextRepository;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<SiteMasterUserPrincipal> current = SecurityUtils.currentPrincipal();
        if (current.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        SiteMasterUserPrincipal principal = current.get();
        Optional<UserSecurityState> state = userRepository.findSecurityState(principal.getUserId());
        if (state.isEmpty() || !state.get().active()) {
            endSession(request);
            errorWriter.write(response, HttpStatus.UNAUTHORIZED, "error.sessionEnded");
            return;
        }

        if (state.get().securityVersion() != principal.getSecurityVersion()) {
            Optional<SiteMasterUserPrincipal> refreshed = principalFactory.createFor(principal.getUserId());
            if (refreshed.isEmpty()) {
                endSession(request);
                errorWriter.write(response, HttpStatus.UNAUTHORIZED, "error.sessionEnded");
                return;
            }
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    refreshed.get(), null, refreshed.get().getAuthorities()));
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
        }
        chain.doFilter(request, response);
    }

    private void endSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
