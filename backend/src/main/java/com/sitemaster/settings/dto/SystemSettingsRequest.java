package com.sitemaster.settings.dto;

import com.sitemaster.validation.ValidSriLankanPhone;
import com.sitemaster.validation.ValidationPatterns;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Request body of PUT /api/admin/settings (everything except the logo).
 * The "start before end" rule for working hours is checked in the service.
 * <p>
 * OOP concept: Encapsulation - private fields, explicit accessors, declarative rules.
 */
public class SystemSettingsRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 100, message = "{company.name.size}")
    private String companyName;

    @Size(max = 2000, message = "{text.size}")
    private String companyDescription;

    @NotBlank(message = "{field.required}")
    @Size(max = 255, message = "{address.size}")
    private String companyAddress;

    @NotBlank(message = "{field.required}")
    @ValidSriLankanPhone
    private String companyPhone;

    @NotBlank(message = "{field.required}")
    @Email(message = "{field.email.invalid}")
    @Pattern(regexp = ValidationPatterns.EMAIL, message = "{field.email.invalid}")
    @Size(max = 100, message = "{field.email.size}")
    private String companyEmail;

    @NotBlank(message = "{field.required}")
    @Pattern(regexp = ValidationPatterns.CURRENCY, message = "{currency.invalid}")
    private String currency;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0.00", message = "{money.min}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal receiptThreshold;

    @NotNull(message = "{field.required}")
    private LocalTime workStartTime;

    @NotNull(message = "{field.required}")
    private LocalTime workEndTime;

    @NotNull(message = "{field.required}")
    @Size(max = 100, message = "{holidays.size}")
    private List<@NotNull(message = "{field.required}") LocalDate> holidays;

    @NotNull(message = "{field.required}")
    @Min(value = 0, message = "{leave.days.range}")
    @Max(value = 365, message = "{leave.days.range}")
    private Integer leaveAnnualDays;

    @NotNull(message = "{field.required}")
    @Min(value = 0, message = "{leave.days.range}")
    @Max(value = 365, message = "{leave.days.range}")
    private Integer leaveCasualDays;

    @NotNull(message = "{field.required}")
    @Min(value = 0, message = "{leave.days.range}")
    @Max(value = 365, message = "{leave.days.range}")
    private Integer leaveSickDays;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0.00", message = "{percent.range}")
    @DecimalMax(value = "100.00", message = "{percent.range}")
    @Digits(integer = 3, fraction = 2, message = "{percent.range}")
    private BigDecimal epfEmployeeRate;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0.00", message = "{percent.range}")
    @DecimalMax(value = "100.00", message = "{percent.range}")
    @Digits(integer = 3, fraction = 2, message = "{percent.range}")
    private BigDecimal epfEmployerRate;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0.00", message = "{percent.range}")
    @DecimalMax(value = "100.00", message = "{percent.range}")
    @Digits(integer = 3, fraction = 2, message = "{percent.range}")
    private BigDecimal etfRate;

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "1.00", message = "{overtime.multiplier.range}")
    @DecimalMax(value = "5.00", message = "{overtime.multiplier.range}")
    @Digits(integer = 1, fraction = 2, message = "{overtime.multiplier.range}")
    private BigDecimal overtimeMultiplier;

    @NotEmpty(message = "{trades.required}")
    @Size(max = 50, message = "{trades.size}")
    private List<@NotBlank(message = "{field.required}")
            @Size(max = 50, message = "{trade.size}")
            @Pattern(regexp = ValidationPatterns.TRADE, message = "{trade.pattern}") String> trades;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyDescription() {
        return companyDescription;
    }

    public void setCompanyDescription(String companyDescription) {
        this.companyDescription = companyDescription;
    }

    public String getCompanyAddress() {
        return companyAddress;
    }

    public void setCompanyAddress(String companyAddress) {
        this.companyAddress = companyAddress;
    }

    public String getCompanyPhone() {
        return companyPhone;
    }

    public void setCompanyPhone(String companyPhone) {
        this.companyPhone = companyPhone;
    }

    public String getCompanyEmail() {
        return companyEmail;
    }

    public void setCompanyEmail(String companyEmail) {
        this.companyEmail = companyEmail;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getReceiptThreshold() {
        return receiptThreshold;
    }

    public void setReceiptThreshold(BigDecimal receiptThreshold) {
        this.receiptThreshold = receiptThreshold;
    }

    public LocalTime getWorkStartTime() {
        return workStartTime;
    }

    public void setWorkStartTime(LocalTime workStartTime) {
        this.workStartTime = workStartTime;
    }

    public LocalTime getWorkEndTime() {
        return workEndTime;
    }

    public void setWorkEndTime(LocalTime workEndTime) {
        this.workEndTime = workEndTime;
    }

    public List<LocalDate> getHolidays() {
        return holidays;
    }

    public void setHolidays(List<LocalDate> holidays) {
        this.holidays = holidays;
    }

    public Integer getLeaveAnnualDays() {
        return leaveAnnualDays;
    }

    public void setLeaveAnnualDays(Integer leaveAnnualDays) {
        this.leaveAnnualDays = leaveAnnualDays;
    }

    public Integer getLeaveCasualDays() {
        return leaveCasualDays;
    }

    public void setLeaveCasualDays(Integer leaveCasualDays) {
        this.leaveCasualDays = leaveCasualDays;
    }

    public Integer getLeaveSickDays() {
        return leaveSickDays;
    }

    public void setLeaveSickDays(Integer leaveSickDays) {
        this.leaveSickDays = leaveSickDays;
    }

    public BigDecimal getEpfEmployeeRate() {
        return epfEmployeeRate;
    }

    public void setEpfEmployeeRate(BigDecimal epfEmployeeRate) {
        this.epfEmployeeRate = epfEmployeeRate;
    }

    public BigDecimal getEpfEmployerRate() {
        return epfEmployerRate;
    }

    public void setEpfEmployerRate(BigDecimal epfEmployerRate) {
        this.epfEmployerRate = epfEmployerRate;
    }

    public BigDecimal getEtfRate() {
        return etfRate;
    }

    public void setEtfRate(BigDecimal etfRate) {
        this.etfRate = etfRate;
    }

    @NotNull(message = "{field.required}")
    @DecimalMin(value = "0.00", message = "{money.min}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal siteAllowancePerDay;

    public BigDecimal getSiteAllowancePerDay() {
        return siteAllowancePerDay;
    }

    public void setSiteAllowancePerDay(BigDecimal siteAllowancePerDay) {
        this.siteAllowancePerDay = siteAllowancePerDay;
    }

    public BigDecimal getOvertimeMultiplier() {
        return overtimeMultiplier;
    }

    public void setOvertimeMultiplier(BigDecimal overtimeMultiplier) {
        this.overtimeMultiplier = overtimeMultiplier;
    }

    @NotEmpty(message = "{materialCategories.required}")
    @Size(max = 50, message = "{materialCategories.size}")
    private List<@NotBlank(message = "{field.required}")
            @Size(max = 50, message = "{trade.size}")
            @Pattern(regexp = ValidationPatterns.TRADE, message = "{materialCategory.pattern}") String> materialCategories;

    @NotEmpty(message = "{materialUnits.required}")
    @Size(max = 30, message = "{materialUnits.size}")
    private List<@NotBlank(message = "{field.required}")
            @Size(max = 20, message = "{materialUnit.size}")
            @Pattern(regexp = ValidationPatterns.UNIT, message = "{materialUnit.pattern}") String> materialUnits;

    public List<String> getMaterialUnits() {
        return materialUnits;
    }

    public void setMaterialUnits(List<String> materialUnits) {
        this.materialUnits = materialUnits;
    }

    public List<String> getMaterialCategories() {
        return materialCategories;
    }

    public void setMaterialCategories(List<String> materialCategories) {
        this.materialCategories = materialCategories;
    }

    public List<String> getTrades() {
        return trades;
    }

    public void setTrades(List<String> trades) {
        this.trades = trades;
    }
}
