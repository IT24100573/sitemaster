package com.sitemaster.issue;

import com.sitemaster.common.enums.IssueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

/** Data access for issues and risks. */
public interface IssueRepository extends JpaRepository<Issue, Long>, JpaSpecificationExecutor<Issue> {

    long countByProjectIdAndStatusIn(Long projectId, Collection<IssueStatus> statuses);
}
