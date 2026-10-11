package com.sitemaster.common.entity;

import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Common shape and approval workflow of every request (MaterialRequest,
 * LeaveRequest, ExpenseRequest).
 * <p>
 * OOP concepts:
 * <ul>
 *   <li>Inheritance - extends BaseEntity; concrete request types extend this.</li>
 *   <li>Polymorphism - subclasses override {@link #approve(User, String)} and
 *       {@link #reject(User, String)} to add their own rules (for example leave
 *       approval deducts the balance) and override {@link #allowedStatuses()} to
 *       limit which statuses they accept.</li>
 *   <li>Encapsulation - status only changes through approve/reject/transitionTo,
 *       so the PENDING and "not your own request" rules cannot be bypassed.</li>
 *   <li>Abstraction - {@link #getRequestType()} is abstract.</li>
 * </ul>
 */
@MappedSuperclass
public abstract class BaseRequest extends BaseEntity {

    /** Minimum length of the reason required when rejecting. */
    public static final int MIN_REJECTION_REMARKS_LENGTH = 5;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false, updatable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_id")
    private User approvedBy;

    @Column(name = "remarks", length = 2000)
    private String remarks;

    protected BaseRequest() {
        // required by JPA
    }

    protected BaseRequest(User requestedBy) {
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy");
    }

    /** Short label such as "Material request", used in notifications and audit entries. */
    public abstract String getRequestType();

    /**
     * Approves a PENDING request. Subclasses override this to add their own rule
     * and must call {@code super.approve(...)}.
     */
    public void approve(User approver, String remarks) {
        ensurePending();
        ensureNotOwnRequest(approver);
        transitionTo(RequestStatus.APPROVED);
        this.approvedBy = approver;
        this.remarks = remarks;
    }

    /**
     * Rejects a PENDING request. A reason of at least
     * {@value #MIN_REJECTION_REMARKS_LENGTH} characters is required.
     */
    public void reject(User approver, String remarks) {
        ensurePending();
        ensureNotOwnRequest(approver);
        if (remarks == null || remarks.trim().length() < MIN_REJECTION_REMARKS_LENGTH) {
            throw BusinessRuleException.forField("remarks", "request.reject.remarksRequired",
                    MIN_REJECTION_REMARKS_LENGTH);
        }
        transitionTo(RequestStatus.REJECTED);
        this.approvedBy = approver;
        this.remarks = remarks.trim();
    }

    /** Statuses this request type may hold. Override to narrow the set. */
    protected Set<RequestStatus> allowedStatuses() {
        return EnumSet.allOf(RequestStatus.class);
    }

    /** Moves to a new status, rejecting values this request type does not support. */
    protected void transitionTo(RequestStatus target) {
        if (!allowedStatuses().contains(target)) {
            throw new InvalidStatusTransitionException("request.status.notAllowed", getRequestType(), target);
        }
        this.status = target;
    }

    public boolean isPending() {
        return status == RequestStatus.PENDING;
    }

    private void ensurePending() {
        if (!isPending()) {
            throw new InvalidStatusTransitionException("request.notPending");
        }
    }

    private void ensureNotOwnRequest(User approver) {
        Objects.requireNonNull(approver, "approver");
        if (requestedBy != null && Objects.equals(requestedBy.getId(), approver.getId())) {
            throw new UnauthorizedActionException("request.approve.own");
        }
    }

    public RequestStatus getStatus() {
        return status;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public String getRemarks() {
        return remarks;
    }
}
