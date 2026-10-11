package com.sitemaster.leave;

import com.sitemaster.common.entity.BaseRequest;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.employee.Employee;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * An employee's leave application, approved or rejected by HR.
 * <p>
 * OOP concepts:
 * <ul>
 *   <li>Inheritance - extends BaseRequest.</li>
 *   <li>Polymorphism - {@link #allowedStatuses()} limits leave to
 *       PENDING/APPROVED/REJECTED, and {@link #approve(User, String)} is overridden
 *       to deduct the days from the leave balance.</li>
 * </ul>
 */
@Entity
@Table(name = "leave_requests")
public class LeaveRequest extends BaseRequest {

    private static final Set<RequestStatus> LEAVE_STATUSES =
            EnumSet.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.REJECTED);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false, updatable = false)
    private LeaveType leaveType;

    @Column(name = "from_date", nullable = false, updatable = false)
    private LocalDate fromDate;

    @Column(name = "to_date", nullable = false, updatable = false)
    private LocalDate toDate;

    @Column(name = "days", nullable = false, precision = 5, scale = 1, updatable = false)
    private BigDecimal days;

    @Column(name = "reason", nullable = false, length = 500, updatable = false)
    private String reason;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** Balance to deduct from, supplied by the service just before approval. */
    @Transient
    private LeaveBalance balanceToDeduct;

    protected LeaveRequest() {
        // required by JPA
    }

    public LeaveRequest(Employee employee, LeaveType leaveType, LocalDate fromDate, LocalDate toDate, BigDecimal days,
                        String reason, User requestedBy) {
        super(requestedBy);
        this.employee = employee;
        this.leaveType = leaveType;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.days = days;
        this.reason = reason;
    }

    @Override
    public String getRequestType() {
        return "Leave request";
    }

    @Override
    protected Set<RequestStatus> allowedStatuses() {
        return LEAVE_STATUSES;
    }

    public void prepareBalance(LeaveBalance balance) {
        this.balanceToDeduct = balance;
    }

    /** Approves (pending, not own) and then deducts the days from the leave balance. */
    @Override
    public void approve(User approver, String remarks) {
        if (balanceToDeduct == null) {
            throw new IllegalStateException("prepareBalance must be called before approve");
        }
        super.approve(approver, remarks);
        balanceToDeduct.deduct(days);
    }

    public void recordDecision(LocalDateTime now) {
        this.decidedAt = now;
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(fromDate) && !date.isAfter(toDate);
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public BigDecimal getDays() {
        return days;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }
}
