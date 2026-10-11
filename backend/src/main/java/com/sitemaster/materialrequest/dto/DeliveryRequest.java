package com.sitemaster.materialrequest.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Site engineer confirms what arrived on site.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class DeliveryRequest {

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{quantity.positive}")
    @Digits(integer = 11, fraction = 3, message = "{quantity.digits}")
    private BigDecimal deliveredQuantity;

    @Size(max = 500, message = "{note.size}")
    private String notes;

    public BigDecimal getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public void setDeliveredQuantity(BigDecimal deliveredQuantity) {
        this.deliveredQuantity = deliveredQuantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
