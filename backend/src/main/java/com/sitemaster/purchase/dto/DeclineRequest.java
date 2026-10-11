package com.sitemaster.purchase.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Supplier's reason for declining a purchase order.
 * <p>
 * OOP concept: Encapsulation - private field with explicit accessors.
 */
public class DeclineRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 5, max = 500, message = "{reason.size}")
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
