package com.sitemaster.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for creating or editing a sub-task.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class SubTaskRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 150, message = "{title.size150}")
    private String title;

    private Long assigneeId;

    private LocalDate dueDate;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
