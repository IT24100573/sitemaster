package com.sitemaster.task.dto;

import com.sitemaster.common.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * New status for a sub-task (workers mark started / completed).
 * <p>
 * OOP concept: Encapsulation - private field with explicit accessors.
 */
public class SubTaskStatusRequest {

    @NotNull(message = "{field.required}")
    private TaskStatus status;

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }
}
