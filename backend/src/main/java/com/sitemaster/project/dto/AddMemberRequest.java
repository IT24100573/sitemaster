package com.sitemaster.project.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for adding a member to a project team.
 * <p>
 * OOP concept: Encapsulation - private field with explicit accessors.
 */
public class AddMemberRequest {

    @NotNull(message = "{field.required}")
    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
