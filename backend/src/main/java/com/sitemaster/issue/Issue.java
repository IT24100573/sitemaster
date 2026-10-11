package com.sitemaster.issue;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.IssueSeverity;
import com.sitemaster.common.enums.IssueStatus;
import com.sitemaster.common.enums.IssueType;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.project.Project;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * A site issue (something wrong now) or risk (something that may go wrong).
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - status moves
 * only along OPEN-&gt;IN_PROGRESS-&gt;RESOLVED-&gt;CLOSED (with re-opening allowed),
 * and RESOLVED/CLOSED need a written resolution.
 */
@Entity
@Table(name = "issues")
public class Issue extends BaseEntity {

    public static final int MIN_RESOLUTION_LENGTH = 5;

    private static final Map<IssueStatus, Set<IssueStatus>> TRANSITIONS = new EnumMap<>(IssueStatus.class);

    static {
        TRANSITIONS.put(IssueStatus.OPEN, EnumSet.of(IssueStatus.IN_PROGRESS, IssueStatus.RESOLVED, IssueStatus.CLOSED));
        TRANSITIONS.put(IssueStatus.IN_PROGRESS, EnumSet.of(IssueStatus.OPEN, IssueStatus.RESOLVED));
        TRANSITIONS.put(IssueStatus.RESOLVED, EnumSet.of(IssueStatus.IN_PROGRESS, IssueStatus.CLOSED));
        TRANSITIONS.put(IssueStatus.CLOSED, EnumSet.noneOf(IssueStatus.class));
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 10)
    private IssueType type;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 10)
    private IssueSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IssueStatus status = IssueStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by_id", nullable = false, updatable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_id")
    private User assignedTo;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "resolution", length = 2000)
    private String resolution;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected Issue() {
        // required by JPA
    }

    public Issue(Project project, User reportedBy) {
        this.project = project;
        this.reportedBy = reportedBy;
    }

    public void update(IssueType type, String title, String description, IssueSeverity severity,
                       User assignedTo, LocalDate dueDate) {
        if (status == IssueStatus.CLOSED) {
            throw new BusinessRuleException("issue.closed");
        }
        this.type = type;
        this.title = title;
        this.description = description;
        this.severity = severity;
        this.assignedTo = assignedTo;
        this.dueDate = dueDate;
    }

    public void changeStatus(IssueStatus target, String resolutionText, LocalDateTime now) {
        if (target == status) {
            return;
        }
        if (!TRANSITIONS.get(status).contains(target)) {
            throw InvalidStatusTransitionException.forField("status", "issue.status.transition", status, target);
        }
        if (target == IssueStatus.RESOLVED || target == IssueStatus.CLOSED) {
            String text = resolutionText != null ? resolutionText.trim() : resolution;
            if (text == null || text.length() < MIN_RESOLUTION_LENGTH) {
                throw BusinessRuleException.forField("resolution", "issue.resolution.required", MIN_RESOLUTION_LENGTH);
            }
            this.resolution = text;
            if (resolvedAt == null) {
                resolvedAt = now;
            }
        } else {
            resolvedAt = null;
        }
        status = target;
    }

    /** Statuses this issue may move to next. */
    public Set<IssueStatus> nextStatuses() {
        return TRANSITIONS.get(status).isEmpty() ? EnumSet.noneOf(IssueStatus.class) : EnumSet.copyOf(TRANSITIONS.get(status));
    }

    public boolean isReportedBy(Long userId) {
        return reportedBy.getId().equals(userId);
    }

    public boolean isAssignedTo(Long userId) {
        return assignedTo != null && assignedTo.getId().equals(userId);
    }

    public Project getProject() {
        return project;
    }

    public IssueType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IssueSeverity getSeverity() {
        return severity;
    }

    public IssueStatus getStatus() {
        return status;
    }

    public User getReportedBy() {
        return reportedBy;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getResolution() {
        return resolution;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
}
