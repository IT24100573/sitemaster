package com.sitemaster.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * Create or edit a shift.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class ShiftRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 50, message = "{shift.name.size}")
    private String name;

    @NotNull(message = "{field.required}")
    private LocalTime startTime;

    @NotNull(message = "{field.required}")
    private LocalTime endTime;

    @Size(max = 255, message = "{address.size}")
    private String description;

    private boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
