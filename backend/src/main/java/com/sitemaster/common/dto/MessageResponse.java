package com.sitemaster.common.dto;

/**
 * Response DTO for actions that only need to report a message.
 * <p>
 * OOP concept: Encapsulation - an immutable value object.
 *
 * @param message human-readable result of the action
 */
public record MessageResponse(String message) {
}
