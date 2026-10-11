package com.sitemaster.task.dto;

import com.sitemaster.common.enums.Urgency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for creating or editing a task. The due date must be inside the
 * phase and the assignee must be on the project (checked in the service).
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class TaskRequest {

    @NotNull(message = "{field.required}")
    private Long phaseId;

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 150, message = "{title.size150}")
    private String title;

    @Size(max = 2000, message = "{text.size}")
    private String description;

    private Long assigneeId;

    @NotNull(message = "{field.required}")
    private Urgency priority;

    @NotNull(message = "{field.required}")
    private LocalDate dueDate;

    public Long getPhaseId() {
        return phaseId;
    }

    public void setPhaseId(Long phaseId) {
        this.phaseId = phaseId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Urgency getPriority() {
        return priority;
    }

    public void setPriority(Urgency priority) {
        this.priority = priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
