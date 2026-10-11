package com.sitemaster.project.dto;

import com.sitemaster.validation.ValidDateRange;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating or editing a phase.
 * <p>
 * OOP concept: Encapsulation - private fields, explicit accessors, declarative rules.
 */
@ValidDateRange(start = "startDate", end = "endDate")
public class PhaseRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 100, message = "{phase.name.size}")
    private String name;

    @Size(max = 2000, message = "{text.size}")
    private String description;

    @NotNull(message = "{field.required}")
    private LocalDate startDate;

    @NotNull(message = "{field.required}")
    private LocalDate endDate;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0.00", message = "{money.min}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal budget;

    @Min(value = 0, message = "{sortOrder.range}")
    @Max(value = 999, message = "{sortOrder.range}")
    private Integer sortOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
