package com.sitemaster.hr.dto;

import com.sitemaster.validation.ValidDateRange;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * HR records an employment contract.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
@ValidDateRange(start = "startDate", end = "endDate")
public class ContractRequest {

    @NotNull(message = "{field.required}")
    private Long employeeId;

    @NotBlank(message = "{field.required}")
    @Pattern(regexp = "^[A-Za-z0-9/\\-]{2,30}$", message = "{contract.number.pattern}")
    private String contractNo;

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 150, message = "{contract.title.size}")
    private String title;

    @NotNull(message = "{field.required}")
    private LocalDate startDate;

    @NotNull(message = "{field.required}")
    private LocalDate endDate;

    @DecimalMin(value = "0", inclusive = false, message = "{money.positive}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal contractValue;

    @Size(max = 500, message = "{note.size}")
    private String notes;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getContractNo() {
        return contractNo;
    }

    public void setContractNo(String contractNo) {
        this.contractNo = contractNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public BigDecimal getContractValue() {
        return contractValue;
    }

    public void setContractValue(BigDecimal contractValue) {
        this.contractValue = contractValue;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
