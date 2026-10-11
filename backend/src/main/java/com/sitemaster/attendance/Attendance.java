package com.sitemaster.attendance;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.AttendanceStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.employee.Employee;
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
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One employee's attendance for one day. A worker's own check-in creates an
 * unverified record; a site engineer or HR verifies or corrects it, and only
 * verified records count for payroll.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - times are
 * validated together (check-out after check-in, at most 16 hours), hours worked
 * are derived, and any correction clears the verification.
 */
@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(name = "uq_attendance_employee_date",
                columnNames = {"employee_id", "attendance_date"}))
public class Attendance extends BaseEntity {

    public static final int MAX_HOURS = 16;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private Shift shift;

    @Column(name = "attendance_date", nullable = false, updatable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(name = "check_in")
    private LocalDateTime checkIn;

    @Column(name = "check_out")
    private LocalDateTime checkOut;

    @Column(name = "hours_worked", precision = 5, scale = 2)
    private BigDecimal hoursWorked;

    @Column(name = "verified", nullable = false)
    private boolean verified;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_id")
    private User verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marked_by_id", nullable = false)
    private User markedBy;

    @Column(name = "notes", length = 500)
    private String notes;

    protected Attendance() {
        // required by JPA
    }

    public Attendance(Employee employee, Project project, LocalDate date, User markedBy) {
        this.employee = employee;
        this.project = project;
        this.date = date;
        this.markedBy = markedBy;
    }

    /** Self check-in: present, not yet verified. */
    public static Attendance checkIn(Employee employee, Project project, LocalDateTime now, User user) {
        Attendance attendance = new Attendance(employee, project, now.toLocalDate(), user);
        attendance.status = AttendanceStatus.PRESENT;
        attendance.checkIn = now;
        return attendance;
    }

    public void checkOut(LocalDateTime now) {
        if (checkIn == null) {
            throw new BusinessRuleException("attendance.checkout.noCheckIn");
        }
        if (checkOut != null) {
            throw new BusinessRuleException("attendance.checkout.done");
        }
        setTimes(checkIn, now);
    }

    /** Full entry or correction by a site engineer or HR. */
    public void record(Project project, Shift shift, AttendanceStatus status, LocalDateTime in, LocalDateTime out,
                       String notes, User by) {
        this.project = project;
        this.shift = shift;
        this.status = status;
        this.notes = notes;
        this.markedBy = by;
        if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.ON_LEAVE) {
            this.checkIn = null;
            this.checkOut = null;
            this.hoursWorked = BigDecimal.ZERO;
        } else {
            setTimes(in, out);
        }
        unverify();
    }

    private void setTimes(LocalDateTime in, LocalDateTime out) {
        if (in != null && !in.toLocalDate().equals(date)) {
            throw BusinessRuleException.forField("checkIn", "attendance.checkin.date");
        }
        if (in != null && out != null) {
            if (!out.isAfter(in)) {
                throw BusinessRuleException.forField("checkOut", "attendance.checkout.order");
            }
            long minutes = Duration.between(in, out).toMinutes();
            if (minutes > MAX_HOURS * 60L) {
                throw BusinessRuleException.forField("checkOut", "attendance.hours.max", MAX_HOURS);
            }
            this.hoursWorked = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        } else {
            this.hoursWorked = null;
        }
        this.checkIn = in;
        this.checkOut = out;
    }

    public void verify(User by, LocalDateTime now) {
        verified = true;
        verifiedBy = by;
        verifiedAt = now;
    }

    private void unverify() {
        verified = false;
        verifiedBy = null;
        verifiedAt = null;
    }

    public boolean isWorkingStatus() {
        return status == AttendanceStatus.PRESENT || status == AttendanceStatus.HALF_DAY;
    }

    public Employee getEmployee() {
        return employee;
    }

    public Project getProject() {
        return project;
    }

    public Shift getShift() {
        return shift;
    }

    public LocalDate getDate() {
        return date;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public LocalDateTime getCheckIn() {
        return checkIn;
    }

    public LocalDateTime getCheckOut() {
        return checkOut;
    }

    public BigDecimal getHoursWorked() {
        return hoursWorked;
    }

    public boolean isVerified() {
        return verified;
    }

    public User getVerifiedBy() {
        return verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public User getMarkedBy() {
        return markedBy;
    }

    public String getNotes() {
        return notes;
    }
}
