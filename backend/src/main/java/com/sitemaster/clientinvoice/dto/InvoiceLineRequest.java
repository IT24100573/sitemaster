package com.sitemaster.clientinvoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * One line of a client invoice.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class InvoiceLineRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 255, message = "{clientInvoice.line.description.size}")
    private String description;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{quantity.positive}")
    @Digits(integer = 11, fraction = 3, message = "{quantity.digits}")
    private BigDecimal quantity;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{money.positive}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal unitPrice;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
