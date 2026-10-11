package com.sitemaster.task;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.task.dto.MyWorkResponse;
import com.sitemaster.task.dto.SubTaskRequest;
import com.sitemaster.task.dto.SubTaskResponse;
import com.sitemaster.task.dto.TaskProgressRequest;
import com.sitemaster.task.dto.TaskRequest;
import com.sitemaster.task.dto.TaskResponse;

/**
 * Tasks and sub-tasks.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface TaskService {

    PageResponse<TaskResponse> list(Long projectId, Long phaseId, TaskStatus status, Long assigneeId, PageQuery pageQuery);

    TaskResponse get(Long taskId);

    TaskResponse create(Long projectId, TaskRequest request);

    TaskResponse update(Long taskId, TaskRequest request);

    TaskResponse updateProgress(Long taskId, TaskProgressRequest request);

    void delete(Long taskId);

    TaskResponse addSubTask(Long taskId, SubTaskRequest request);

    TaskResponse updateSubTask(Long subTaskId, SubTaskRequest request);

    SubTaskResponse changeSubTaskStatus(Long subTaskId, TaskStatus status);

    TaskResponse deleteSubTask(Long subTaskId);

    MyWorkResponse myWork();
}
