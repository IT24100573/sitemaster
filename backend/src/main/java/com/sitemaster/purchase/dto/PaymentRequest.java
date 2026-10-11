package com.sitemaster.purchase.dto;

import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.validation.NotFutureDate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Accountant records a payment. Bank transfers and cheques need a unique
 * reference (checked in the service and by a database index).
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class PaymentRequest {

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{money.positive}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal amount;

    @NotNull(message = "{field.required}")
    private PaymentMethod method;

    @Pattern(regexp = "^[A-Za-z0-9/\\-]{3,50}$", message = "{payment.reference.pattern}")
    private String referenceNo;

    @NotNull(message = "{field.required}")
    @NotFutureDate
    private LocalDate paymentDate;

    @Size(max = 500, message = "{note.size}")
    private String notes;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
