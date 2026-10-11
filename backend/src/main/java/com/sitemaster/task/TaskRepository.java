package com.sitemaster.task;

import com.sitemaster.common.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data access for tasks. */
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    long countByPhaseIdAndStatusNot(Long phaseId, TaskStatus status);

    long countByPhaseId(Long phaseId);

    List<Task> findByPhaseId(Long phaseId);

    long countByProjectIdAndAssigneeIdAndStatusNot(Long projectId, Long assigneeId, TaskStatus status);

    @Query("select coalesce(avg(t.progress), 0) from Task t where t.project.id = :projectId")
    double averageProgress(@Param("projectId") Long projectId);

    @Query("select count(t) from Task t where t.project.id = :projectId")
    long countByProject(@Param("projectId") Long projectId);

    @Query("select t from Task t join fetch t.project p join fetch t.phase "
            + "where t.assignee.id = :userId and p.status <> com.sitemaster.common.enums.ProjectStatus.ARCHIVED "
            + "order by t.dueDate asc")
    List<Task> findAssignedTo(@Param("userId") Long userId);
}
