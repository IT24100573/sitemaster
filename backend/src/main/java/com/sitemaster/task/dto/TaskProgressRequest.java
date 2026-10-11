package com.sitemaster.task.dto;

import com.sitemaster.common.enums.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Status and percentage update (also used by Kanban drag and drop).
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class TaskProgressRequest {

    @NotNull(message = "{field.required}")
    private TaskStatus status;

    @NotNull(message = "{field.required}")
    @Min(value = 0, message = "{percent.whole}")
    @Max(value = 100, message = "{percent.whole}")
    private Integer progress;

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }
}
