package com.sitemaster.task;

import com.sitemaster.common.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data access for sub-tasks. */
public interface SubTaskRepository extends JpaRepository<SubTask, Long> {

    @Query("select s from SubTask s join fetch s.task t join fetch t.project p "
            + "where s.assignee.id = :userId and p.status <> com.sitemaster.common.enums.ProjectStatus.ARCHIVED "
            + "order by s.dueDate asc nulls last")
    List<SubTask> findAssignedTo(@Param("userId") Long userId);

    @Query("select count(s) from SubTask s where s.task.project.id = :projectId "
            + "and s.assignee.id = :userId and s.status <> :done")
    long countOpenForUser(@Param("projectId") Long projectId, @Param("userId") Long userId,
                          @Param("done") TaskStatus done);
}
