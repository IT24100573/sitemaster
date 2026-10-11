package com.sitemaster.payroll.dto;

import com.sitemaster.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Accountant pays salaries; bank transfers and cheques need a reference.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class SalaryPaymentRequest {

    @NotNull(message = "{field.required}")
    private PaymentMethod method;

    @Pattern(regexp = "^[A-Za-z0-9/\\-]{3,50}$", message = "{payment.reference.pattern}")
    private String reference;

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }
}
