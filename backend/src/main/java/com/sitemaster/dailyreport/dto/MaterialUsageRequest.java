package com.sitemaster.dailyreport.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * One material used on the day.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class MaterialUsageRequest {

    @NotNull(message = "{field.required}")
    private Long materialId;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{quantity.positive}")
    @Digits(integer = 11, fraction = 3, message = "{quantity.digits}")
    private BigDecimal quantity;

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }
}
