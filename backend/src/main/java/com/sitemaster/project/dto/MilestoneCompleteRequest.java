package com.sitemaster.project.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Marks a milestone as completed or not.
 * <p>
 * OOP concept: Encapsulation - private field with explicit accessors.
 */
public class MilestoneCompleteRequest {

    @NotNull(message = "{field.required}")
    private Boolean completed;

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }
}
