package com.sitemaster.equipment.dto;

/**
 * Assign equipment to a project, or release it when {@code projectId} is null.
 * <p>
 * OOP concept: Encapsulation - private field with explicit accessors.
 */
public class EquipmentAssignmentRequest {

    private Long projectId;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
}
