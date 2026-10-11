package com.sitemaster.attendance;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Overtime hours recorded against one attendance record (0 &lt; hours &lt;= 8),
 * approved by HR before payroll uses them.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - hours can only
 * change while pending; decisions happen once.
 */
@Entity
@Table(name = "overtime")
public class Overtime extends BaseEntity {

    public static final BigDecimal MAX_HOURS = BigDecimal.valueOf(8);

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attendance_id", nullable = false, unique = true, updatable = false)
    private Attendance attendance;

    @Column(name = "hours", nullable = false, precision = 4, scale = 2)
    private BigDecimal hours;

    @Column(name = "reason", length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private User recordedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_id")
    private User decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    protected Overtime() {
        // required by JPA
    }

    public Overtime(Attendance attendance, User recordedBy) {
        this.attendance = attendance;
        this.recordedBy = recordedBy;
    }

    public void update(BigDecimal hours, String reason, User by) {
        if (status != RequestStatus.PENDING) {
            throw new InvalidStatusTransitionException("overtime.decided");
        }
        if (!attendance.isWorkingStatus()) {
            throw BusinessRuleException.forField("overtimeHours", "overtime.notPresent");
        }
        if (hours == null || hours.signum() <= 0 || hours.compareTo(MAX_HOURS) > 0) {
            throw BusinessRuleException.forField("overtimeHours", "overtime.hours.range");
        }
        this.hours = hours;
        this.reason = reason;
        this.recordedBy = by;
    }

    public void decide(boolean approve, User by, String note, LocalDateTime now) {
        if (status != RequestStatus.PENDING) {
            throw new InvalidStatusTransitionException("request.notPending");
        }
        if (!approve && (note == null || note.trim().length() < 5)) {
            throw BusinessRuleException.forField("remarks", "request.reject.remarksRequired", 5);
        }
        status = approve ? RequestStatus.APPROVED : RequestStatus.REJECTED;
        decidedBy = by;
        decidedAt = now;
        decisionNote = note;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public String getReason() {
        return reason;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public User getDecidedBy() {
        return decidedBy;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public String getDecisionNote() {
        return decisionNote;
    }
}
