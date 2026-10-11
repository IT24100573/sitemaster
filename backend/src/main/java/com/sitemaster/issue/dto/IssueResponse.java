package com.sitemaster.issue.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** An issue or risk with the statuses it may move to next. */
public record IssueResponse(Long id,
                            Long projectId,
                            String projectName,
                            String type,
                            String title,
                            String description,
                            String severity,
                            String status,
                            List<String> nextStatuses,
                            Long reportedById,
                            String reportedByName,
                            Long assignedToId,
                            String assignedToName,
                            LocalDate dueDate,
                            String resolution,
                            LocalDateTime resolvedAt,
                            LocalDateTime createdAt) {

    public IssueResponse {
        nextStatuses = List.copyOf(nextStatuses);
    }
}
