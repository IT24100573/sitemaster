package com.sitemaster.payroll;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.PayrollStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
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
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * One month's payroll: HR sends the attendance summary (SUMMARY_SENT, which locks
 * the month's attendance), the Accountant calculates (CALCULATED) and pays (PAID).
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - one run per
 * month, steps only in order, and a PAID run is read-only.
 */
@Entity
@Table(name = "payroll_runs", uniqueConstraints = @UniqueConstraint(name = "uq_payroll_runs_month",
        columnNames = {"year", "month"}))
public class PayrollRun extends BaseEntity {

    @Column(name = "year", nullable = false, updatable = false)
    private int year;

    @Column(name = "month", nullable = false, updatable = false)
    private int month;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PayrollStatus status = PayrollStatus.SUMMARY_SENT;

    @Column(name = "working_days", nullable = false, updatable = false)
    private int workingDays;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "summary_sent_by_id", nullable = false, updatable = false)
    private User summarySentBy;

    @Column(name = "summary_sent_at", nullable = false, updatable = false)
    private LocalDateTime summarySentAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculated_by_id")
    private User calculatedBy;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "total_gross", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalGross = BigDecimal.ZERO;

    @Column(name = "total_deductions", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    @Column(name = "total_net", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalNet = BigDecimal.ZERO;

    protected PayrollRun() {
        // required by JPA
    }

    public PayrollRun(YearMonth period, int workingDays, User summarySentBy, LocalDateTime now) {
        this.year = period.getYear();
        this.month = period.getMonthValue();
        this.workingDays = workingDays;
        this.summarySentBy = summarySentBy;
        this.summarySentAt = now;
    }

    /** Calculation is allowed after HR sent the summary, and again to recalculate before payment. */
    public void ensureCalculable() {
        if (status == PayrollStatus.PAID) {
            throw new InvalidStatusTransitionException("payroll.paid.readOnly");
        }
    }

    public void markCalculated(User by, LocalDateTime now, BigDecimal gross, BigDecimal deductions, BigDecimal net) {
        ensureCalculable();
        status = PayrollStatus.CALCULATED;
        calculatedBy = by;
        calculatedAt = now;
        totalGross = gross;
        totalDeductions = deductions;
        totalNet = net;
    }

    public void ensurePayable() {
        if (status == PayrollStatus.SUMMARY_SENT) {
            throw new BusinessRuleException("payroll.notCalculated");
        }
        if (status == PayrollStatus.PAID) {
            throw new InvalidStatusTransitionException("payroll.paid.readOnly");
        }
    }

    public void markPaid(LocalDateTime now) {
        status = PayrollStatus.PAID;
        paidAt = now;
    }

    public YearMonth period() {
        return YearMonth.of(year, month);
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public PayrollStatus getStatus() {
        return status;
    }

    public int getWorkingDays() {
        return workingDays;
    }

    public User getSummarySentBy() {
        return summarySentBy;
    }

    public LocalDateTime getSummarySentAt() {
        return summarySentAt;
    }

    public User getCalculatedBy() {
        return calculatedBy;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public BigDecimal getTotalGross() {
        return totalGross;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public BigDecimal getTotalNet() {
        return totalNet;
    }
}
