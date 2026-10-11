package com.sitemaster.employee;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Employment record of a user (1-1): NIC, trade, employment type and pay terms.
 * Name, email and phone live on the User.
 * <p>
 * OOP concepts:
 * <ul>
 *   <li>Inheritance - extends BaseEntity.</li>
 *   <li>Encapsulation - pay terms can only be set through {@link #applyPayTerms},
 *       which enforces the rule for each employment type and clears amounts that
 *       do not apply, so the record can never hold an inconsistent combination.</li>
 * </ul>
 */
@Entity
@Table(name = "employees")
public class Employee extends BaseEntity {

    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 65;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true, updatable = false)
    private User user;

    @Column(name = "employee_no", nullable = false, unique = true, length = 20, updatable = false)
    private String employeeNo;

    @Column(name = "nic", nullable = false, unique = true, length = 12)
    private String nic;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType;

    @Column(name = "trade", length = 50)
    private String trade;

    @Column(name = "daily_rate", precision = 14, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "monthly_salary", precision = 14, scale = 2)
    private BigDecimal monthlySalary;

    @Column(name = "contract_amount", precision = 14, scale = 2)
    private BigDecimal contractAmount;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 15)
    private String emergencyContactPhone;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<EmployeeDocument> documents = new ArrayList<>();

    protected Employee() {
        // required by JPA
    }

    public Employee(User user, String employeeNo) {
        this.user = user;
        this.employeeNo = employeeNo;
    }

    /**
     * Sets the employment type and its pay terms.
     * DAILY_WAGE needs a daily rate &gt; 0, PERMANENT a monthly salary &gt; 0, and
     * CONTRACT a contract amount &gt; 0 plus start and end dates (end &gt;= start).
     */
    public void applyPayTerms(EmploymentType type, BigDecimal dailyRate, BigDecimal monthlySalary,
                              BigDecimal contractAmount, LocalDate contractStart, LocalDate contractEnd) {
        if (type == null) {
            throw BusinessRuleException.forField("employmentType", "field.required");
        }
        this.employmentType = type;
        this.dailyRate = null;
        this.monthlySalary = null;
        this.contractAmount = null;
        this.contractStartDate = null;
        this.contractEndDate = null;

        switch (type) {
            case DAILY_WAGE -> this.dailyRate = requirePositive(dailyRate, "dailyRate");
            case PERMANENT -> this.monthlySalary = requirePositive(monthlySalary, "monthlySalary");
            case CONTRACT -> {
                this.contractAmount = requirePositive(contractAmount, "contractAmount");
                if (contractStart == null) {
                    throw BusinessRuleException.forField("contractStartDate", "employee.contract.datesRequired");
                }
                if (contractEnd == null) {
                    throw BusinessRuleException.forField("contractEndDate", "employee.contract.datesRequired");
                }
                if (contractEnd.isBefore(contractStart)) {
                    throw BusinessRuleException.forField("contractEndDate", "daterange.invalid");
                }
                this.contractStartDate = contractStart;
                this.contractEndDate = contractEnd;
            }
        }
    }

    /** Sets the date of birth after checking the age is within 18-65 today. */
    public void changeDateOfBirth(LocalDate dateOfBirth, LocalDate today) {
        int age = Period.between(dateOfBirth, today).getYears();
        if (dateOfBirth.isAfter(today) || age < MIN_AGE || age > MAX_AGE) {
            throw BusinessRuleException.forField("dateOfBirth", "employee.age.range", MIN_AGE, MAX_AGE);
        }
        this.dateOfBirth = dateOfBirth;
    }

    public int ageOn(LocalDate today) {
        return Period.between(dateOfBirth, today).getYears();
    }

    public void addDocument(EmployeeDocument document) {
        documents.add(document);
    }

    public void removeDocument(EmployeeDocument document) {
        documents.remove(document);
    }

    private static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw BusinessRuleException.forField(field, "employee.pay.required");
        }
        return value;
    }

    // ----- getters / setters ---------------------------------------------------

    public User getUser() {
        return user;
    }

    public String getEmployeeNo() {
        return employeeNo;
    }

    public String getNic() {
        return nic;
    }

    /** NICs are stored uppercase (old-format V/X suffix). */
    public void setNic(String nic) {
        this.nic = nic == null ? null : nic.trim().toUpperCase(Locale.ROOT);
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
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

    public String getTrade() {
        return trade;
    }

    public void setTrade(String trade) {
        this.trade = trade;
    }

    public BigDecimal getDailyRate() {
        return dailyRate;
    }

    public BigDecimal getMonthlySalary() {
        return monthlySalary;
    }

    public BigDecimal getContractAmount() {
        return contractAmount;
    }

    public LocalDate getContractStartDate() {
        return contractStartDate;
    }

    public LocalDate getContractEndDate() {
        return contractEndDate;
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

    public List<EmployeeDocument> getDocuments() {
        return Collections.unmodifiableList(documents);
    }
}
