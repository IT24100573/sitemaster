package com.sitemaster.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an entity is asked to move to a status it cannot reach from its current status (HTTP 422).
 * <p>
 * OOP concept: Inheritance / Polymorphism - specialises ApplicationException with
 * its own HTTP status.
 */
public class InvalidStatusTransitionException extends ApplicationException {

    /** Form-level error. */
    public InvalidStatusTransitionException(String messageKey, Object... args) {
        super(null, messageKey, args);
    }

    /** Error attached to one request field. */
    public static InvalidStatusTransitionException forField(String field, String messageKey, Object... args) {
        return new InvalidStatusTransitionException(field, messageKey, args);
    }

    private InvalidStatusTransitionException(String field, String messageKey, Object[] args) {
        super(field, messageKey, args);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
