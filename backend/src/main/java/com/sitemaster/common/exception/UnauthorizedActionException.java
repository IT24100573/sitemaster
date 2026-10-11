package com.sitemaster.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when the current user is signed in but may not perform this action on this record (HTTP 403).
 * <p>
 * OOP concept: Inheritance / Polymorphism - specialises ApplicationException with
 * its own HTTP status.
 */
public class UnauthorizedActionException extends ApplicationException {

    /** Form-level error. */
    public UnauthorizedActionException(String messageKey, Object... args) {
        super(null, messageKey, args);
    }

    /** Error attached to one request field. */
    public static UnauthorizedActionException forField(String field, String messageKey, Object... args) {
        return new UnauthorizedActionException(field, messageKey, args);
    }

    private UnauthorizedActionException(String field, String messageKey, Object[] args) {
        super(field, messageKey, args);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.FORBIDDEN;
    }
}
