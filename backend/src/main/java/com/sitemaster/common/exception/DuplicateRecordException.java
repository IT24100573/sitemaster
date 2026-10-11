package com.sitemaster.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a create or update would duplicate a unique value (HTTP 409).
 * <p>
 * OOP concept: Inheritance / Polymorphism - specialises ApplicationException with
 * its own HTTP status.
 */
public class DuplicateRecordException extends ApplicationException {

    /** Form-level error. */
    public DuplicateRecordException(String messageKey, Object... args) {
        super(null, messageKey, args);
    }

    /** Error attached to one request field. */
    public static DuplicateRecordException forField(String field, String messageKey, Object... args) {
        return new DuplicateRecordException(field, messageKey, args);
    }

    private DuplicateRecordException(String field, String messageKey, Object[] args) {
        super(field, messageKey, args);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.CONFLICT;
    }
}
