package com.sitemaster.auth;

import com.sitemaster.auth.dto.ForgotPasswordRequest;
import com.sitemaster.auth.dto.LoginRequest;
import com.sitemaster.auth.dto.ResetPasswordRequest;
import com.sitemaster.security.SiteMasterUserPrincipal;

/**
 * Sign-in and password recovery.
 * <p>
 * OOP concept: Abstraction - the controller depends only on this interface;
 * {@link AuthServiceImpl} is the implementation.
 */
public interface AuthService {

    /**
     * Checks the credentials and returns the principal to store in the session.
     *
     * @throws com.sitemaster.common.exception.AuthenticationFailedException when
     *         the credentials are wrong, the account is locked or it is disabled
     */
    SiteMasterUserPrincipal authenticate(LoginRequest request);

    /**
     * Creates a one-time reset link for an active account and logs it to the
     * console. Does nothing visible when the email is unknown, so the response
     * never reveals whether an account exists.
     */
    void requestPasswordReset(ForgotPasswordRequest request);

    /** Sets a new password using a valid, unused, unexpired reset token. */
    void resetPassword(ResetPasswordRequest request);

    /**
     * Issues a reset link for a user and returns the raw token. Used by
     * "forgot password" and, in later phases, by supplier approval to send a
     * set-password link.
     */
    String issuePasswordResetLink(Long userId);
}
