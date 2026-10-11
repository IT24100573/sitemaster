package com.sitemaster.leave;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.settings.CompanySettings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * A kind of leave. ANNUAL, CASUAL and SICK take their yearly entitlement from
 * System Settings (days_per_year is empty); other types store their own.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - the
 * entitlement is answered by {@link #entitlement(CompanySettings)}.
 */
@Entity
@Table(name = "leave_types")
public class LeaveType extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "days_per_year", precision = 5, scale = 1)
    private BigDecimal daysPerYear;

    @Column(name = "paid", nullable = false)
    private boolean paid = true;

    @Column(name = "backdate_days", nullable = false)
    private int backdateDays;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected LeaveType() {
        // required by JPA
    }

    public LeaveType(String code) {
        this.code = code.trim().toUpperCase(Locale.ROOT);
    }

    public void update(String name, BigDecimal daysPerYear, boolean paid, int backdateDays, boolean active) {
        this.name = name;
        this.daysPerYear = isFromSettings() ? null : daysPerYear;
        this.paid = paid;
        this.backdateDays = backdateDays;
        this.active = active;
    }

    /** True for ANNUAL, CASUAL and SICK, whose days come from System Settings. */
    public boolean isFromSettings() {
        return "ANNUAL".equals(code) || "CASUAL".equals(code) || "SICK".equals(code);
    }

    public BigDecimal entitlement(CompanySettings settings) {
        return switch (code) {
            case "ANNUAL" -> BigDecimal.valueOf(settings.leaveAnnualDays());
            case "CASUAL" -> BigDecimal.valueOf(settings.leaveCasualDays());
            case "SICK" -> BigDecimal.valueOf(settings.leaveSickDays());
            default -> daysPerYear == null ? BigDecimal.ZERO : daysPerYear;
        };
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getDaysPerYear() {
        return daysPerYear;
    }

    public boolean isPaid() {
        return paid;
    }

    public int getBackdateDays() {
        return backdateDays;
    }

    public boolean isActive() {
        return active;
    }
}
