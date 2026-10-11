package com.sitemaster.clientinvoice;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.common.exception.BusinessRuleException;
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
 * Money received from a client against an invoice.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - non-cash
 * receipts need a reference; immutable.
 */
@Entity
@Table(name = "client_payments")
public class ClientPayment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private ClientInvoice invoice;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "payment_date", nullable = false, updatable = false)
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20, updatable = false)
    private PaymentMethod method;

    @Column(name = "reference_no", length = 50, updatable = false)
    private String referenceNo;

    @Column(name = "notes", length = 500, updatable = false)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "received_by_id", nullable = false, updatable = false)
    private User receivedBy;

    protected ClientPayment() {
        // required by JPA
    }

    public ClientPayment(ClientInvoice invoice, BigDecimal amount, LocalDate paymentDate, PaymentMethod method,
                         String referenceNo, String notes, User receivedBy) {
        if (method != PaymentMethod.CASH && (referenceNo == null || referenceNo.isBlank())) {
            throw BusinessRuleException.forField("referenceNo", "payment.reference.required");
        }
        this.invoice = invoice;
        this.amount = amount;
        this.paymentDate = paymentDate;
        this.method = method;
        this.referenceNo = method == PaymentMethod.CASH || referenceNo == null ? null
                : referenceNo.trim().toUpperCase(Locale.ROOT);
        this.notes = notes;
        this.receivedBy = receivedBy;
    }

    public ClientInvoice getInvoice() {
        return invoice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public String getNotes() {
        return notes;
    }

    public User getReceivedBy() {
        return receivedBy;
    }
}
