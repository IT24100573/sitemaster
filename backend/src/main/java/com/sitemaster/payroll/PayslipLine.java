package com.sitemaster.payroll;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.payroll.decorator.PayslipEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * One saved step of a payslip calculation (base, overtime, allowance, deduction)
 * with the running total after it.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable).
 */
@Entity
@Table(name = "payslip_lines")
public class PayslipLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payslip_id", nullable = false, updatable = false)
    private Payslip payslip;

    @Column(name = "line_order", nullable = false, updatable = false)
    private int lineOrder;

    @Column(name = "line_type", nullable = false, length = 20, updatable = false)
    private String lineType;

    @Column(name = "label", nullable = false, length = 150, updatable = false)
    private String label;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "running_total", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal runningTotal;

    protected PayslipLine() {
        // required by JPA
    }

    public PayslipLine(Payslip payslip, int lineOrder, PayslipEntry entry) {
        this.payslip = payslip;
        this.lineOrder = lineOrder;
        this.lineType = entry.type().name();
        this.label = entry.label();
        this.amount = entry.amount();
        this.runningTotal = entry.runningTotal();
    }

    public Payslip getPayslip() {
        return payslip;
    }

    public int getLineOrder() {
        return lineOrder;
    }

    public String getLineType() {
        return lineType;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getRunningTotal() {
        return runningTotal;
    }
}
