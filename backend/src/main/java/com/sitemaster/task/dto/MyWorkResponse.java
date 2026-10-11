package com.sitemaster.task.dto;

import java.util.List;

/**
 * Everything assigned to the signed-in user.
 *
 * @param tasks    tasks assigned to them
 * @param subTasks sub-tasks assigned to them
 */
public record MyWorkResponse(List<TaskResponse> tasks, List<SubTaskResponse> subTasks) {

    public MyWorkResponse {
        tasks = List.copyOf(tasks);
        subTasks = List.copyOf(subTasks);
    }
}
