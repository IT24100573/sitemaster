package com.sitemaster.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class of every business exception thrown by the service layer.
 * It carries a message KEY (resolved from ValidationMessages.properties by the
 * GlobalExceptionHandler) instead of hard-coded text, plus an optional field name
 * so the frontend can show the error under the matching form field.
 * <p>
 * OOP concepts: Inheritance and Polymorphism - each subclass overrides
 * {@link #getStatus()} and the handler treats them all through this type.
 */
public abstract class ApplicationException extends RuntimeException {

    private final String field;
    private final String messageKey;
    private final Object[] args;

    protected ApplicationException(String field, String messageKey, Object... args) {
        super(messageKey);
        this.field = field;
        this.messageKey = messageKey;
        this.args = args == null ? new Object[0] : args.clone();
    }

    /** HTTP status returned to the client for this kind of failure. */
    public abstract HttpStatus getStatus();

    /** Name of the request field this error belongs to, or null for a form-level error. */
    public String getField() {
        return field;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getArgs() {
        return args.clone();
    }
}
