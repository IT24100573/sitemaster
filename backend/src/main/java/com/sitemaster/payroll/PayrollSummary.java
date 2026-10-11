package com.sitemaster.payroll;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * HR's monthly attendance summary for one employee: verified attendance,
 * approved leave and approved overtime. It is what the Accountant pays from.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable).
 */
@Entity
@Table(name = "payroll_summaries")
public class PayrollSummary extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false, updatable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20, updatable = false)
    private EmploymentType employmentType;

    @Column(name = "present_days", nullable = false, updatable = false)
    private int presentDays;

    @Column(name = "half_days", nullable = false, updatable = false)
    private int halfDays;

    @Column(name = "absent_days", nullable = false, updatable = false)
    private int absentDays;

    @Column(name = "paid_leave_days", nullable = false, precision = 5, scale = 1, updatable = false)
    private BigDecimal paidLeaveDays;

    @Column(name = "unpaid_leave_days", nullable = false, precision = 5, scale = 1, updatable = false)
    private BigDecimal unpaidLeaveDays;

    @Column(name = "overtime_hours", nullable = false, precision = 6, scale = 2, updatable = false)
    private BigDecimal overtimeHours;

    protected PayrollSummary() {
        // required by JPA
    }

    public PayrollSummary(PayrollRun payrollRun, Employee employee, AttendanceTotals totals) {
        this.payrollRun = payrollRun;
        this.employee = employee;
        this.employmentType = employee.getEmploymentType();
        this.presentDays = totals.presentDays();
        this.halfDays = totals.halfDays();
        this.absentDays = totals.absentDays();
        this.paidLeaveDays = totals.paidLeaveDays();
        this.unpaidLeaveDays = totals.unpaidLeaveDays();
        this.overtimeHours = totals.overtimeHours();
    }

    public AttendanceTotals totals() {
        return new AttendanceTotals(presentDays, halfDays, absentDays, paidLeaveDays, unpaidLeaveDays, overtimeHours);
    }

    public PayrollRun getPayrollRun() {
        return payrollRun;
    }

    public Employee getEmployee() {
        return employee;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }
}
