package com.sitemaster.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is well-formed but breaks a business rule (HTTP 422).
 * <p>
 * OOP concept: Inheritance / Polymorphism - specialises ApplicationException with
 * its own HTTP status.
 */
public class BusinessRuleException extends ApplicationException {

    /** Form-level error. */
    public BusinessRuleException(String messageKey, Object... args) {
        super(null, messageKey, args);
    }

    /** Error attached to one request field. */
    public static BusinessRuleException forField(String field, String messageKey, Object... args) {
        return new BusinessRuleException(field, messageKey, args);
    }

    private BusinessRuleException(String field, String messageKey, Object[] args) {
        super(field, messageKey, args);
    }

    @Override
    public HttpStatus getStatus() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
