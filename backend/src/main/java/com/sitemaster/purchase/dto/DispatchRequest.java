package com.sitemaster.purchase.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Supplier marks an order dispatched with the expected delivery date.
 * <p>
 * OOP concept: Encapsulation - private field with explicit accessors.
 */
public class DispatchRequest {

    @NotNull(message = "{field.required}")
    private LocalDate expectedDeliveryDate;

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }
}
