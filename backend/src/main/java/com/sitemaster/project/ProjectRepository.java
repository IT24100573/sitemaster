package com.sitemaster.project;

import com.sitemaster.common.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** Data access for projects. */
public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    long countByClientId(Long clientId);

    @Query("select count(p) > 0 from Project p where lower(p.name) = lower(:name) "
            + "and p.status <> com.sitemaster.common.enums.ProjectStatus.ARCHIVED and (:excludeId is null or p.id <> :excludeId)")
    boolean existsOpenWithName(@Param("name") String name, @Param("excludeId") Long excludeId);

    List<Project> findByStatusInOrderByNameAsc(Collection<ProjectStatus> statuses);

    List<Project> findByStatusOrderByCompletedDateDesc(ProjectStatus status);
}
