package com.sitemaster.attendance.dto;

import jakarta.validation.constraints.NotNull;

/**
 * A worker or site engineer checks in at one of their projects.
 * <p>
 * OOP concept: Encapsulation - private field with explicit accessors.
 */
public class CheckInRequest {

    @NotNull(message = "{field.required}")
    private Long projectId;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
