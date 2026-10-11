package com.sitemaster.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for milestones. */
public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByProjectIdOrderByDueDateAsc(Long projectId);
}
