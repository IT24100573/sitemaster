package com.sitemaster.task;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.task.dto.MyWorkResponse;
import com.sitemaster.task.dto.SubTaskRequest;
import com.sitemaster.task.dto.SubTaskResponse;
import com.sitemaster.task.dto.SubTaskStatusRequest;
import com.sitemaster.task.dto.TaskProgressRequest;
import com.sitemaster.task.dto.TaskRequest;
import com.sitemaster.task.dto.TaskResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tasks, sub-tasks and "my tasks". */
@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/api/projects/{projectId}/tasks")
    @PreAuthorize("hasAuthority('TASK:VIEW')")
    public PageResponse<TaskResponse> list(@PathVariable Long projectId,
                                           @RequestParam(required = false) Long phaseId,
                                           @RequestParam(required = false) TaskStatus status,
                                           @RequestParam(required = false) Long assigneeId,
                                           @Valid @ModelAttribute PageQuery pageQuery) {
        return taskService.list(projectId, phaseId, status, assigneeId, pageQuery);
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TASK:CREATE')")
    public TaskResponse create(@PathVariable Long projectId, @Valid @RequestBody TaskRequest request) {
        return taskService.create(projectId, request);
    }

    @GetMapping("/api/tasks/mine")
    @PreAuthorize("hasAuthority('TASK:VIEW')")
    public MyWorkResponse mine() {
        return taskService.myWork();
    }

    @GetMapping("/api/tasks/{taskId}")
    @PreAuthorize("hasAuthority('TASK:VIEW')")
    public TaskResponse get(@PathVariable Long taskId) {
        return taskService.get(taskId);
    }

    @PutMapping("/api/tasks/{taskId}")
    @PreAuthorize("hasAuthority('TASK:EDIT')")
    public TaskResponse update(@PathVariable Long taskId, @Valid @RequestBody TaskRequest request) {
        return taskService.update(taskId, request);
    }

    @PatchMapping("/api/tasks/{taskId}/progress")
    @PreAuthorize("hasAuthority('TASK:EDIT')")
    public TaskResponse updateProgress(@PathVariable Long taskId, @Valid @RequestBody TaskProgressRequest request) {
        return taskService.updateProgress(taskId, request);
    }

    @DeleteMapping("/api/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('TASK:DELETE')")
    public void delete(@PathVariable Long taskId) {
        taskService.delete(taskId);
    }

    @PostMapping("/api/tasks/{taskId}/subtasks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('TASK:CREATE')")
    public TaskResponse addSubTask(@PathVariable Long taskId, @Valid @RequestBody SubTaskRequest request) {
        return taskService.addSubTask(taskId, request);
    }

    @PutMapping("/api/subtasks/{subTaskId}")
    @PreAuthorize("hasAuthority('TASK:CREATE')")
    public TaskResponse updateSubTask(@PathVariable Long subTaskId, @Valid @RequestBody SubTaskRequest request) {
        return taskService.updateSubTask(subTaskId, request);
    }

    @PatchMapping("/api/subtasks/{subTaskId}/status")
    @PreAuthorize("hasAuthority('TASK:EDIT')")
    public SubTaskResponse changeSubTaskStatus(@PathVariable Long subTaskId,
                                               @Valid @RequestBody SubTaskStatusRequest request) {
        return taskService.changeSubTaskStatus(subTaskId, request.getStatus());
    }

    @DeleteMapping("/api/subtasks/{subTaskId}")
    @PreAuthorize("hasAuthority('TASK:CREATE')")
    public TaskResponse deleteSubTask(@PathVariable Long subTaskId) {
        return taskService.deleteSubTask(subTaskId);
    }
}
