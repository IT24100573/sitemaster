package com.sitemaster.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Generic status change: the target status name plus an optional note
 * (e.g. an issue's resolution). The service converts the name to its enum, so
 * an unknown value is reported as a field error.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class StatusChangeRequest {

    @NotBlank(message = "{field.required}")
    @Size(max = 30, message = "{status.invalid}")
    private String status;

    @Size(max = 2000, message = "{text.size}")
    private String note;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
