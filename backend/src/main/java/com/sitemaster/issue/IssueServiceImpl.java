package com.sitemaster.issue;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.IssueSeverity;
import com.sitemaster.common.enums.IssueStatus;
import com.sitemaster.common.enums.IssueType;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.common.util.EnumParser;
import com.sitemaster.issue.dto.IssueRequest;
import com.sitemaster.issue.dto.IssueResponse;
import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.project.ProjectMemberRepository;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link IssueService}. Anyone on the project (with the ISSUE
 * permissions) may report; the PM and the reporter may edit; the PM, reporter or
 * assignee may change the status. Reporting notifies the PM; status changes
 * notify the reporter.
 * <p>
 * OOP concept: Abstraction - implements IssueService.
 */
@Service
public class IssueServiceImpl implements IssueService {

    private static final Map<String, String> SORTS = Map.of(
            "createdAt", "createdAt", "severity", "severity", "status", "status", "dueDate", "dueDate", "title", "title");

    private final IssueRepository issueRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectAccess access;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public IssueServiceImpl(IssueRepository issueRepository, ProjectMemberRepository memberRepository,
                            UserRepository userRepository, ProjectAccess access, CurrentUserProvider currentUser,
                            WorkflowEventPublisher eventPublisher, Clock clock) {
        this.issueRepository = issueRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<IssueResponse> list(Long projectId, IssueType type, IssueStatus status, IssueSeverity severity,
                                            PageQuery pageQuery) {
        access.requireVisible(projectId);
        String search = pageQuery.getSearch();
        Specification<Issue> spec = (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
        spec = spec.and((root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type))
                .and((root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status))
                .and((root, query, cb) -> severity == null ? null : cb.equal(root.get("severity"), severity))
                .and((root, query, cb) -> search == null ? null
                        : cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase(Locale.ROOT) + "%"));
        return PageResponse.from(issueRepository.findAll(spec, pageQuery.toPageable(SORTS, "createdAt", Sort.Direction.DESC)),
                IssueServiceImpl::toResponse);
    }

    @Override
    @Transactional
    public IssueResponse report(Long projectId, IssueRequest request) {
        Project project = access.requireParticipant(projectId);
        if (!access.isManager(project) && !access.isMember(project)) {
            throw new UnauthorizedActionException("issue.report.denied");
        }
        Issue issue = new Issue(project, currentUser.requireUser());
        User assignee = resolveMember(project, request.getAssignedToId());
        issue.update(request.getType(), request.getTitle(), request.getDescription(), request.getSeverity(),
                assignee, request.getDueDate());
        issueRepository.save(issue);

        WorkflowEvent.Builder event = WorkflowEvent.byCurrentUser(WorkflowEventType.ISSUE_REPORTED)
                .entityId(issue.getId())
                .args(issue.getType().name().toLowerCase(Locale.ROOT), issue.getTitle(), issue.getSeverity(),
                        project.getName())
                .link("projects/" + projectId);
        Long me = currentUser.requireId();
        if (!project.isManagedBy(me)) {
            event.recipients(project.getManager().getId());
        }
        if (assignee != null && !assignee.getId().equals(me)) {
            event.recipients(assignee.getId());
        }
        eventPublisher.publish(event.build());
        return toResponse(issue);
    }

    @Override
    @Transactional
    public IssueResponse update(Long issueId, IssueRequest request) {
        Issue issue = find(issueId);
        Project project = access.requireParticipant(issue.getProject().getId());
        if (!access.isManager(project) && !issue.isReportedBy(currentUser.requireId())) {
            throw new UnauthorizedActionException("issue.edit.denied");
        }
        User assignee = resolveMember(project, request.getAssignedToId());
        issue.update(request.getType(), request.getTitle(), request.getDescription(), request.getSeverity(),
                assignee, request.getDueDate());
        WorkflowEvent.Builder event = WorkflowEvent.byCurrentUser(WorkflowEventType.ISSUE_UPDATED)
                .entityId(issue.getId()).args(issue.getTitle(), project.getName()).link("projects/" + project.getId());
        if (assignee != null && !assignee.getId().equals(currentUser.requireId())) {
            event.recipients(assignee.getId());
        }
        eventPublisher.publish(event.build());
        return toResponse(issue);
    }

    @Override
    @Transactional
    public IssueResponse changeStatus(Long issueId, String status, String resolution) {
        IssueStatus target = EnumParser.parse(IssueStatus.class, status, "status");
        Issue issue = find(issueId);
        Project project = access.requireParticipant(issue.getProject().getId());
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        if (!access.isManager(me, project) && !issue.isReportedBy(me.getUserId())
                && !issue.isAssignedTo(me.getUserId())) {
            throw new UnauthorizedActionException("issue.status.denied");
        }
        IssueStatus previous = issue.getStatus();
        issue.changeStatus(target, resolution, LocalDateTime.now(clock));
        WorkflowEvent.Builder event = WorkflowEvent.byCurrentUser(WorkflowEventType.ISSUE_STATUS_CHANGED)
                .entityId(issue.getId()).args(issue.getTitle(), previous, target, project.getName())
                .link("projects/" + project.getId());
        if (!issue.isReportedBy(me.getUserId())) {
            event.recipients(issue.getReportedBy().getId());
        }
        eventPublisher.publish(event.build());
        return toResponse(issue);
    }

    private User resolveMember(Project project, Long userId) {
        if (userId == null) {
            return null;
        }
        if (!memberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            throw BusinessRuleException.forField("assignedToId", "task.assignee.notMember");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> BusinessRuleException.forField("assignedToId", "task.assignee.notMember"));
    }

    private Issue find(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Issue"));
    }

    private static IssueResponse toResponse(Issue i) {
        return new IssueResponse(i.getId(), i.getProject().getId(), i.getProject().getName(), i.getType().name(),
                i.getTitle(), i.getDescription(), i.getSeverity().name(), i.getStatus().name(),
                i.nextStatuses().stream().map(Enum::name).toList(), i.getReportedBy().getId(),
                i.getReportedBy().getFullName(), i.getAssignedTo() == null ? null : i.getAssignedTo().getId(),
                i.getAssignedTo() == null ? null : i.getAssignedTo().getFullName(), i.getDueDate(),
                i.getResolution(), i.getResolvedAt(), i.getCreatedAt());
    }
}
