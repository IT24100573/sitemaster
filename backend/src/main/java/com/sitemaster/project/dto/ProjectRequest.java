package com.sitemaster.project.dto;

import com.sitemaster.validation.ValidDateRange;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating or editing a project.
 * <p>
 * OOP concept: Encapsulation - private fields, explicit accessors, declarative rules.
 */
@ValidDateRange(start = "startDate", end = "endDate")
public class ProjectRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 150, message = "{project.name.size}")
    private String name;

    @Size(max = 2000, message = "{text.size}")
    private String description;

    @NotNull(message = "{field.required}")
    private Long clientId;

    @NotNull(message = "{field.required}")
    private Long managerId;

    @NotBlank(message = "{field.required}")
    @Size(max = 255, message = "{address.size}")
    private String location;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "-90", message = "{latitude.range}")
    @DecimalMax(value = "90", message = "{latitude.range}")
    @Digits(integer = 3, fraction = 6, message = "{coordinate.digits}")
    private BigDecimal latitude;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "-180", message = "{longitude.range}")
    @DecimalMax(value = "180", message = "{longitude.range}")
    @Digits(integer = 3, fraction = 6, message = "{coordinate.digits}")
    private BigDecimal longitude;

    @NotNull(message = "{field.required}")
    private LocalDate startDate;

    @NotNull(message = "{field.required}")
    private LocalDate endDate;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0", inclusive = false, message = "{money.positive}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal budget;

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

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getManagerId() {
        return managerId;
    }

    public void setManagerId(Long managerId) {
        this.managerId = managerId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
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
}
