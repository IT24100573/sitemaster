package com.sitemaster.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested record does not exist or is not visible to the current user (HTTP 404).
 * <p>
 * OOP concept: Inheritance / Polymorphism - specialises ApplicationException with
 * its own HTTP status.
 */
public class ResourceNotFoundException extends ApplicationException {

    /** Form-level error. */
    public ResourceNotFoundException(String messageKey, Object... args) {
        super(null, messageKey, args);
    }

    /** Error attached to one request field. */
    public static ResourceNotFoundException forField(String field, String messageKey, Object... args) {
        return new ResourceNotFoundException(field, messageKey, args);
    }

    private ResourceNotFoundException(String field, String messageKey, Object[] args) {
        super(field, messageKey, args);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
