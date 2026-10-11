package com.sitemaster.issue;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.IssueSeverity;
import com.sitemaster.common.enums.IssueStatus;
import com.sitemaster.common.enums.IssueType;
import com.sitemaster.issue.dto.IssueRequest;
import com.sitemaster.issue.dto.IssueResponse;

/**
 * Issues and risks of a project.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface IssueService {

    PageResponse<IssueResponse> list(Long projectId, IssueType type, IssueStatus status, IssueSeverity severity,
                                     PageQuery pageQuery);

    IssueResponse report(Long projectId, IssueRequest request);

    IssueResponse update(Long issueId, IssueRequest request);

    IssueResponse changeStatus(Long issueId, String status, String resolution);
}
