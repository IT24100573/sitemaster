package com.sitemaster.expense.dto;

import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.validation.NotFutureDate;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Accountant pays an approved expense, from the bank or from the project's
 * petty cash (which is always cash).
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class ExpensePaymentRequest {

    private boolean fromPettyCash;

    @NotNull(message = "{field.required}")
    private PaymentMethod method;

    @Pattern(regexp = "^[A-Za-z0-9/\\-]{3,50}$", message = "{payment.reference.pattern}")
    private String referenceNo;

    @NotNull(message = "{field.required}")
    @NotFutureDate
    private LocalDate paymentDate;

    @Size(max = 500, message = "{note.size}")
    private String notes;

    public boolean isFromPettyCash() {
        return fromPettyCash;
    }

    public void setFromPettyCash(boolean fromPettyCash) {
        this.fromPettyCash = fromPettyCash;
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
