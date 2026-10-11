package com.sitemaster.purchase;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.expense.ExpenseRequest;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

/**
 * Money paid out by the Accountant: settles either a supplier invoice or an
 * approved expense request (never both).
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - bank
 * transfers and cheques must carry a reference; payments are immutable.
 */
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_invoice_id", updatable = false)
    private SupplierInvoice supplierInvoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_request_id", updatable = false)
    private ExpenseRequest expenseRequest;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20, updatable = false)
    private PaymentMethod method;

    @Column(name = "reference_no", length = 50, updatable = false)
    private String referenceNo;

    @Column(name = "payment_date", nullable = false, updatable = false)
    private LocalDate paymentDate;

    @Column(name = "notes", length = 500, updatable = false)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_id", nullable = false, updatable = false)
    private User paidBy;

    protected Payment() {
        // required by JPA
    }

    public Payment(BigDecimal amount, PaymentMethod method, String referenceNo, LocalDate paymentDate, String notes,
                   User paidBy) {
        if (method != PaymentMethod.CASH && (referenceNo == null || referenceNo.isBlank())) {
            throw BusinessRuleException.forField("referenceNo", "payment.reference.required");
        }
        this.amount = amount;
        this.method = method;
        this.referenceNo = referenceNo == null ? null : referenceNo.trim().toUpperCase(Locale.ROOT);
        this.paymentDate = paymentDate;
        this.notes = notes;
        this.paidBy = paidBy;
    }

    public void settle(SupplierInvoice invoice) {
        this.supplierInvoice = invoice;
    }

    public void settle(ExpenseRequest expense) {
        this.expenseRequest = expense;
    }

    public ExpenseRequest getExpenseRequest() {
        return expenseRequest;
    }

    public SupplierInvoice getSupplierInvoice() {
        return supplierInvoice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public String getNotes() {
        return notes;
    }

    public User getPaidBy() {
        return paidBy;
    }
}
