package com.sitemaster.material.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request body for creating or editing a catalogue material. Category and unit
 * must be values from System Settings (checked in the service).
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class MaterialRequestBody {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 100, message = "{material.name.size}")
    private String name;

    @NotBlank(message = "{field.required}")
    @Size(max = 50, message = "{trade.size}")
    private String category;

    @NotBlank(message = "{field.required}")
    @Size(max = 20, message = "{materialUnit.size}")
    private String unit;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0.00", message = "{money.min}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal unitPrice;

    @Size(max = 500, message = "{note.size}")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
