package com.sitemaster.task.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A sub-task, with enough context for a worker's "My tasks" list. */
public record SubTaskResponse(Long id,
                              Long taskId,
                              String taskTitle,
                              Long projectId,
                              String projectName,
                              String title,
                              Long assigneeId,
                              String assigneeName,
                              String status,
                              LocalDate dueDate,
                              LocalDateTime completedAt) {
}
