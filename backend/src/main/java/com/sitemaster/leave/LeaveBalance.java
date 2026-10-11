package com.sitemaster.leave;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.employee.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * Days of one leave type an employee is entitled to and has used in a year.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - days are only
 * deducted through {@link #deduct}, which refuses to go over the entitlement.
 */
@Entity
@Table(name = "leave_balances",
        uniqueConstraints = @UniqueConstraint(name = "uq_leave_balances",
                columnNames = {"employee_id", "leave_type_id", "year"}))
public class LeaveBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leave_type_id", nullable = false, updatable = false)
    private LeaveType leaveType;

    @Column(name = "year", nullable = false, updatable = false)
    private int year;

    @Column(name = "entitled_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal entitledDays;

    @Column(name = "used_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal usedDays = BigDecimal.ZERO;

    protected LeaveBalance() {
        // required by JPA
    }

    public LeaveBalance(Employee employee, LeaveType leaveType, int year, BigDecimal entitledDays) {
        this.employee = employee;
        this.leaveType = leaveType;
        this.year = year;
        this.entitledDays = entitledDays;
    }

    public BigDecimal remaining() {
        return entitledDays.subtract(usedDays);
    }

    public void deduct(BigDecimal days) {
        if (days.compareTo(remaining()) > 0) {
            throw BusinessRuleException.forField("toDate", "leave.balance.insufficient", remaining());
        }
        usedDays = usedDays.add(days);
    }

    public Employee getEmployee() {
        return employee;
    }

    public LeaveType getLeaveType() {
        return leaveType;
    }

    public int getYear() {
        return year;
    }

    public BigDecimal getEntitledDays() {
        return entitledDays;
    }

    public BigDecimal getUsedDays() {
        return usedDays;
    }
}
