package com.sitemaster.common.exception;

import java.util.List;
import java.util.Map;

/**
 * The one error body returned by every failing API call:
 * { "status", "message", "fieldErrors": { field: message }, "globalErrors": [] }.
 * <p>
 * OOP concept: Encapsulation - an immutable value object (record) whose
 * collections are defensively copied.
 */
public record ErrorResponse(int status,
                            String message,
                            Map<String, String> fieldErrors,
                            List<String> globalErrors) {

    public ErrorResponse {
        fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
        globalErrors = globalErrors == null ? List.of() : List.copyOf(globalErrors);
    }

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, Map.of(), List.of());
    }
}
