package com.sitemaster.auth;

import com.sitemaster.auth.dto.CurrentUserResponse;
import com.sitemaster.auth.dto.ForgotPasswordRequest;
import com.sitemaster.auth.dto.LoginRequest;
import com.sitemaster.auth.dto.ResetPasswordRequest;
import com.sitemaster.common.dto.MessageResponse;
import com.sitemaster.common.util.MessageResolver;
import com.sitemaster.security.SessionAuthenticationHelper;
import com.sitemaster.security.SiteMasterUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. Logout (POST /api/auth/logout) is handled by
 * Spring Security's logout filter configured in SecurityConfig.
 * <p>
 * OOP concept: Abstraction - depends on the AuthService interface only and
 * contains no business logic.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionAuthenticationHelper sessionHelper;
    private final MessageResolver messages;

    public AuthController(AuthService authService,
                          SessionAuthenticationHelper sessionHelper,
                          MessageResolver messages) {
        this.authService = authService;
        this.sessionHelper = sessionHelper;
        this.messages = messages;
    }

    /** Makes sure the XSRF-TOKEN cookie is present before the first POST. */
    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf() {
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public CurrentUserResponse login(@Valid @RequestBody LoginRequest body,
                                     HttpServletRequest request, HttpServletResponse response) {
        SiteMasterUserPrincipal principal = authService.authenticate(body);
        sessionHelper.establishSession(principal, request, response);
        return AuthMapper.toCurrentUser(principal);
    }

    @GetMapping("/me")
    public CurrentUserResponse me(@AuthenticationPrincipal SiteMasterUserPrincipal principal) {
        return AuthMapper.toCurrentUser(principal);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        authService.requestPasswordReset(body);
        return new MessageResponse(messages.get("auth.forgotPassword.sent"));
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest body) {
        authService.resetPassword(body);
        return new MessageResponse(messages.get("auth.resetPassword.success"));
    }
}
