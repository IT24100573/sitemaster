package com.sitemaster.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Data access for project membership. */
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    @Query("select m from ProjectMember m join fetch m.user u join fetch u.role "
            + "where m.project.id = :projectId order by u.fullName")
    List<ProjectMember> findWithUsers(@Param("projectId") Long projectId);

    @Query("select count(m) from ProjectMember m where m.project.id = :projectId "
            + "and m.user.role.name = com.sitemaster.common.enums.RoleName.WORKER")
    long countWorkers(@Param("projectId") Long projectId);

    @Query("select m.user.id from ProjectMember m where m.project.id = :projectId")
    List<Long> findUserIds(@Param("projectId") Long projectId);
}
