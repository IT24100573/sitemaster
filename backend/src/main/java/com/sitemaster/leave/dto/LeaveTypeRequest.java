package com.sitemaster.leave.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * HR creates or edits a leave type. The code is fixed after creation; days for
 * ANNUAL, CASUAL and SICK come from System Settings.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class LeaveTypeRequest {

    @NotBlank(message = "{field.required}")
    @Pattern(regexp = "^[A-Za-z_]{2,20}$", message = "{leaveType.code.pattern}")
    private String code;

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 50, message = "{leaveType.name.size}")
    private String name;

    @DecimalMin(value = "0", message = "{leaveType.days.range}")
    @DecimalMax(value = "365", message = "{leaveType.days.range}")
    @Digits(integer = 3, fraction = 1, message = "{leaveType.days.range}")
    private BigDecimal daysPerYear;

    private boolean paid = true;

    @Min(value = 0, message = "{leaveType.backdate.range}")
    @Max(value = 31, message = "{leaveType.backdate.range}")
    private int backdateDays;

    private boolean active = true;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getDaysPerYear() {
        return daysPerYear;
    }

    public void setDaysPerYear(BigDecimal daysPerYear) {
        this.daysPerYear = daysPerYear;
    }

    public boolean isPaid() {
        return paid;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public int getBackdateDays() {
        return backdateDays;
    }

    public void setBackdateDays(int backdateDays) {
        this.backdateDays = backdateDays;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
