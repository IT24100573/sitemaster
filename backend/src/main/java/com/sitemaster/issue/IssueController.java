package com.sitemaster.issue;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.IssueSeverity;
import com.sitemaster.common.enums.IssueStatus;
import com.sitemaster.common.enums.IssueType;
import com.sitemaster.issue.dto.IssueRequest;
import com.sitemaster.issue.dto.IssueResponse;
import com.sitemaster.project.dto.StatusChangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** Issues and risks. */
@RestController
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping("/api/projects/{projectId}/issues")
    @PreAuthorize("hasAuthority('ISSUE:VIEW')")
    public PageResponse<IssueResponse> list(@PathVariable Long projectId,
                                            @RequestParam(required = false) IssueType type,
                                            @RequestParam(required = false) IssueStatus status,
                                            @RequestParam(required = false) IssueSeverity severity,
                                            @Valid @ModelAttribute PageQuery pageQuery) {
        return issueService.list(projectId, type, status, severity, pageQuery);
    }

    @PostMapping("/api/projects/{projectId}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ISSUE:CREATE')")
    public IssueResponse report(@PathVariable Long projectId, @Valid @RequestBody IssueRequest request) {
        return issueService.report(projectId, request);
    }

    @PutMapping("/api/issues/{issueId}")
    @PreAuthorize("hasAuthority('ISSUE:EDIT')")
    public IssueResponse update(@PathVariable Long issueId, @Valid @RequestBody IssueRequest request) {
        return issueService.update(issueId, request);
    }

    @PatchMapping("/api/issues/{issueId}/status")
    @PreAuthorize("hasAuthority('ISSUE:EDIT')")
    public IssueResponse changeStatus(@PathVariable Long issueId, @Valid @RequestBody StatusChangeRequest request) {
        return issueService.changeStatus(issueId, request.getStatus(), request.getNote());
    }
}
