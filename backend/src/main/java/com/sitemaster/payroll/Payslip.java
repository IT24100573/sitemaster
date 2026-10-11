package com.sitemaster.payroll;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.employee.Employee;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * One employee's pay for a payroll run, with every calculation step as a line.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - net pay can
 * never be negative and a payslip can only be paid once.
 */
@Entity
@Table(name = "payslips")
public class Payslip extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payroll_run_id", nullable = false, updatable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20, updatable = false)
    private EmploymentType employmentType;

    @Column(name = "gross_pay", nullable = false, precision = 14, scale = 2)
    private BigDecimal grossPay;

    @Column(name = "total_deductions", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "net_pay", nullable = false, precision = 14, scale = 2)
    private BigDecimal netPay;

    @Column(name = "paid", nullable = false)
    private boolean paid;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_reference", length = 50)
    private String paymentReference;

    @OneToMany(mappedBy = "payslip", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<PayslipLine> lines = new ArrayList<>();

    protected Payslip() {
        // required by JPA
    }

    public Payslip(PayrollRun run, Employee employee, BigDecimal grossPay, BigDecimal totalDeductions, BigDecimal netPay) {
        if (netPay.signum() < 0) {
            throw new BusinessRuleException("payroll.net.negative", employee.getEmployeeNo());
        }
        this.payrollRun = run;
        this.employee = employee;
        this.employmentType = employee.getEmploymentType();
        this.grossPay = grossPay;
        this.totalDeductions = totalDeductions;
        this.netPay = netPay;
    }

    public void addLine(PayslipLine line) {
        lines.add(line);
    }

    public void pay(PaymentMethod method, String reference, LocalDateTime now) {
        if (paid) {
            throw new BusinessRuleException("payslip.alreadyPaid");
        }
        if (method != PaymentMethod.CASH && (reference == null || reference.isBlank())) {
            throw BusinessRuleException.forField("reference", "payment.reference.required");
        }
        paid = true;
        paidAt = now;
        paymentMethod = method;
        paymentReference = reference == null ? null : reference.trim().toUpperCase(Locale.ROOT);
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

    public BigDecimal getGrossPay() {
        return grossPay;
    }

    public BigDecimal getTotalDeductions() {
        return totalDeductions;
    }

    public BigDecimal getNetPay() {
        return netPay;
    }

    public boolean isPaid() {
        return paid;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public List<PayslipLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
