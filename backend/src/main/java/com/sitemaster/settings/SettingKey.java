package com.sitemaster.settings;

/**
 * Keys of the rows in the system_settings table. Default values are inserted by
 * Flyway (V3) and edited by the Admin on the System Settings page.
 * <p>
 * OOP concept: Encapsulation - the database key string is private to the enum.
 */
public enum SettingKey {
    COMPANY_NAME("company.name"),
    COMPANY_DESCRIPTION("company.description"),
    COMPANY_ADDRESS("company.address"),
    COMPANY_PHONE("company.phone"),
    COMPANY_EMAIL("company.email"),
    COMPANY_LOGO_FILE_ID("company.logoFileId"),
    CURRENCY("finance.currency"),
    RECEIPT_THRESHOLD("finance.receiptThreshold"),
    WORK_START_TIME("work.startTime"),
    WORK_END_TIME("work.endTime"),
    HOLIDAYS("work.holidays"),
    LEAVE_ANNUAL_DAYS("leave.annualDays"),
    LEAVE_CASUAL_DAYS("leave.casualDays"),
    LEAVE_SICK_DAYS("leave.sickDays"),
    EPF_EMPLOYEE_RATE("payroll.epfEmployeeRate"),
    EPF_EMPLOYER_RATE("payroll.epfEmployerRate"),
    ETF_RATE("payroll.etfRate"),
    OVERTIME_MULTIPLIER("payroll.overtimeMultiplier"),
    SITE_ALLOWANCE_PER_DAY("payroll.siteAllowancePerDay"),
    EMPLOYEE_TRADES("employee.trades"),
    MATERIAL_CATEGORIES("material.categories"),
    MATERIAL_UNITS("material.units");

    private final String key;

    SettingKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
