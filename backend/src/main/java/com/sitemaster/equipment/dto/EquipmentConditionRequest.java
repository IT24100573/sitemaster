package com.sitemaster.equipment.dto;

import com.sitemaster.common.enums.EquipmentStatus;
import com.sitemaster.validation.NotFutureDate;
import com.sitemaster.validation.ValidDateRange;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Status and service details reported for a piece of equipment.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
@ValidDateRange(start = "lastServiceDate", end = "nextServiceDate")
public class EquipmentConditionRequest {

    @NotNull(message = "{field.required}")
    private EquipmentStatus status;

    @Size(max = 500, message = "{note.size}")
    private String notes;

    @NotFutureDate
    private LocalDate lastServiceDate;

    private LocalDate nextServiceDate;

    public EquipmentStatus getStatus() {
        return status;
    }

    public void setStatus(EquipmentStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDate getLastServiceDate() {
        return lastServiceDate;
    }

    public void setLastServiceDate(LocalDate lastServiceDate) {
        this.lastServiceDate = lastServiceDate;
    }

    public LocalDate getNextServiceDate() {
        return nextServiceDate;
    }

    public void setNextServiceDate(LocalDate nextServiceDate) {
        this.nextServiceDate = nextServiceDate;
    }
}
