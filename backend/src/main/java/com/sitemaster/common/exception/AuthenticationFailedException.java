package com.sitemaster.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when sign-in fails. Static factories keep the three failure reasons
 * (bad credentials, locked, disabled) in one place.
 * <p>
 * OOP concepts: Inheritance and Encapsulation - constructors are private, so the
 * only way to create one is through a named factory method.
 */
public class AuthenticationFailedException extends ApplicationException {

    private AuthenticationFailedException(String messageKey, Object... args) {
        super(null, messageKey, args);
    }

    /** Same message whether the email or the password was wrong. */
    public static AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException("auth.invalidCredentials");
    }

    public static AuthenticationFailedException accountLocked(long minutesRemaining) {
        return new AuthenticationFailedException("auth.accountLocked", minutesRemaining);
    }

    public static AuthenticationFailedException accountDisabled() {
        return new AuthenticationFailedException("auth.accountDisabled");
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNAUTHORIZED;
    }
}
