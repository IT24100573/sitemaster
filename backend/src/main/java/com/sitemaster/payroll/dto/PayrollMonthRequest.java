package com.sitemaster.payroll.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Payroll month chosen by HR.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class PayrollMonthRequest {

    @NotNull(message = "{field.required}")
    @Min(value = 2000, message = "{year.range}")
    @Max(value = 2100, message = "{year.range}")
    private Integer year;

    @NotNull(message = "{field.required}")
    @Min(value = 1, message = "{month.range}")
    @Max(value = 12, message = "{month.range}")
    private Integer month;

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }
}
