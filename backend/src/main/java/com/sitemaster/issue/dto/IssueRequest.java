package com.sitemaster.issue.dto;

import com.sitemaster.common.enums.IssueSeverity;
import com.sitemaster.common.enums.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request body for reporting or editing an issue or risk.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class IssueRequest {

    @NotNull(message = "{field.required}")
    private IssueType type;

    @NotBlank(message = "{field.required}")
    @Size(min = 5, max = 150, message = "{issue.title.size}")
    private String title;

    @NotBlank(message = "{field.required}")
    @Size(min = 10, max = 2000, message = "{issue.description.size}")
    private String description;

    @NotNull(message = "{field.required}")
    private IssueSeverity severity;

    private Long assignedToId;

    private LocalDate dueDate;

    public IssueType getType() {
        return type;
    }

    public void setType(IssueType type) {
        this.type = type;
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

    public IssueSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(IssueSeverity severity) {
        this.severity = severity;
    }

    public Long getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(Long assignedToId) {
        this.assignedToId = assignedToId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}
