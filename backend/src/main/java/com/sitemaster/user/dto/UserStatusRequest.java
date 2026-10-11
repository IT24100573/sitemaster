package com.sitemaster.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body of PATCH /api/admin/users/{id}/status.
 * <p>
 * OOP concept: Encapsulation - private field with explicit accessors.
 */
public class UserStatusRequest {

    @NotNull(message = "{field.required}")
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
