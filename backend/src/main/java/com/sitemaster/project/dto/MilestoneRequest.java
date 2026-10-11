package com.sitemaster.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for creating or editing a milestone.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class MilestoneRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 150, message = "{title.size150}")
    private String name;

    @NotNull(message = "{field.required}")
    private LocalDate dueDate;

    private Long phaseId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public Long getPhaseId() {
        return phaseId;
    }

    public void setPhaseId(Long phaseId) {
        this.phaseId = phaseId;
    }
}
