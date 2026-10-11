package com.sitemaster.employee.dto;

import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.validation.NotFutureDate;
import com.sitemaster.validation.ValidDateRange;
import com.sitemaster.validation.ValidNIC;
import com.sitemaster.validation.ValidSriLankanPhone;
import com.sitemaster.validation.ValidationPatterns;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fields shared by the create and update employee requests.
 * Pay rules per employment type, the 18-65 age rule, trade rules and NIC
 * uniqueness on update are checked in the service and the Employee entity.
 * <p>
 * OOP concepts: Inheritance (EmployeeCreateRequest and EmployeeUpdateRequest
 * extend this abstract class) and Encapsulation (private fields with accessors).
 */
@ValidDateRange(start = "contractStartDate", end = "contractEndDate")
public abstract class EmployeeDetailsRequest {

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 100, message = "{name.size}")
    @Pattern(regexp = ValidationPatterns.PERSON_NAME, message = "{name.pattern}")
    private String fullName;

    @NotBlank(message = "{field.required}")
    @ValidSriLankanPhone
    private String phone;

    @NotBlank(message = "{field.required}")
    @ValidNIC
    private String nic;

    @NotNull(message = "{field.required}")
    @Past(message = "{dob.past}")
    private LocalDate dateOfBirth;

    @NotBlank(message = "{field.required}")
    @Size(max = 255, message = "{address.size}")
    private String address;

    @NotNull(message = "{field.required}")
    @NotFutureDate(message = "{joinDate.future}")
    private LocalDate joinDate;

    @NotNull(message = "{field.required}")
    private EmploymentType employmentType;

    @Size(max = 50, message = "{trade.size}")
    private String trade;

    @DecimalMin(value = "0", inclusive = false, message = "{money.positive}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal dailyRate;

    @DecimalMin(value = "0", inclusive = false, message = "{money.positive}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal monthlySalary;

    @DecimalMin(value = "0", inclusive = false, message = "{money.positive}")
    @Digits(integer = 12, fraction = 2, message = "{money.digits}")
    private BigDecimal contractAmount;

    private LocalDate contractStartDate;

    private LocalDate contractEndDate;

    @Size(min = 2, max = 100, message = "{name.size}")
    @Pattern(regexp = ValidationPatterns.PERSON_NAME, message = "{name.pattern}")
    private String emergencyContactName;

    @ValidSriLankanPhone
    private String emergencyContactPhone;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNic() {
        return nic;
    }

    public void setNic(String nic) {
        this.nic = nic;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public EmploymentType getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(EmploymentType employmentType) {
        this.employmentType = employmentType;
    }

    public String getTrade() {
        return trade;
    }

    public void setTrade(String trade) {
        this.trade = trade;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public void setDailyRate(BigDecimal dailyRate) {
        this.dailyRate = dailyRate;
    }

    public BigDecimal getMonthlySalary() {
        return monthlySalary;
    }

    public void setMonthlySalary(BigDecimal monthlySalary) {
        this.monthlySalary = monthlySalary;
    }

    public BigDecimal getContractAmount() {
        return contractAmount;
    }

    public void setContractAmount(BigDecimal contractAmount) {
        this.contractAmount = contractAmount;
    }

    public LocalDate getContractStartDate() {
        return contractStartDate;
    }

    public void setContractStartDate(LocalDate contractStartDate) {
        this.contractStartDate = contractStartDate;
    }

    public LocalDate getContractEndDate() {
        return contractEndDate;
    }

    public void setContractEndDate(LocalDate contractEndDate) {
        this.contractEndDate = contractEndDate;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }
}
