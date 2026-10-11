package com.sitemaster.materialrequest.dto;

import com.sitemaster.common.enums.Urgency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A site engineer's material request. "Required by" must be today or later
 * (checked in the service with the application clock).
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class MaterialRequestCreate {

    @NotNull(message = "{field.required}")
    private Long phaseId;

    @NotNull(message = "{field.required}")
    private Long materialId;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{quantity.positive}")
    @Digits(integer = 11, fraction = 3, message = "{quantity.digits}")
    private BigDecimal quantity;

    @NotNull(message = "{field.required}")
    private Urgency urgency;

    @NotNull(message = "{field.required}")
    private LocalDate requiredBy;

    @Size(max = 500, message = "{note.size}")
    private String notes;

    public Long getPhaseId() {
        return phaseId;
    }

    public void setPhaseId(Long phaseId) {
        this.phaseId = phaseId;
    }

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

    public Urgency getUrgency() {
        return urgency;
    }

    public void setUrgency(Urgency urgency) {
        this.urgency = urgency;
    }

    public LocalDate getRequiredBy() {
        return requiredBy;
    }

    public void setRequiredBy(LocalDate requiredBy) {
        this.requiredBy = requiredBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
