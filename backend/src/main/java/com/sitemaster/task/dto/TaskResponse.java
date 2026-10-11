package com.sitemaster.task.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** A task with its sub-tasks. */
public record TaskResponse(Long id,
                           Long projectId,
                           String projectName,
                           Long phaseId,
                           String phaseName,
                           String title,
                           String description,
                           Long assigneeId,
                           String assigneeName,
                           String priority,
                           LocalDate dueDate,
                           String status,
                           int progress,
                           LocalDateTime completedAt,
                           boolean overdue,
                           List<SubTaskResponse> subTasks) {

    public TaskResponse {
        subTasks = List.copyOf(subTasks);
    }
}
