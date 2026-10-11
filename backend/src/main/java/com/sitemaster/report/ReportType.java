package com.sitemaster.report;

import com.sitemaster.common.enums.RoleName;

import java.util.EnumSet;
import java.util.Set;

/**
 * The reports SiteMaster can produce, who may run each, and whether it needs a
 * project when run by someone limited to their own projects.
 * <p>
 * OOP concept: Encapsulation - each report carries its own title and audience.
 */
public enum ReportType {
    PROJECT_FINANCE("Project finance", "Budget, spending, commitments and billing per project",
            EnumSet.of(RoleName.ADMIN, RoleName.ACCOUNTANT, RoleName.PROJECT_MANAGER)),
    EXPENSES("Expenses", "Expense requests by date, project and category",
            EnumSet.of(RoleName.ADMIN, RoleName.ACCOUNTANT, RoleName.PROJECT_MANAGER, RoleName.SITE_ENGINEER)),
    PAYMENTS("Payments", "Supplier and expense payments made",
            EnumSet.of(RoleName.ADMIN, RoleName.ACCOUNTANT)),
    PAYROLL("Payroll", "Payslips of the months in the period",
            EnumSet.of(RoleName.ADMIN, RoleName.HR_MANAGER, RoleName.ACCOUNTANT)),
    ATTENDANCE("Attendance", "Attendance totals per employee",
            EnumSet.of(RoleName.ADMIN, RoleName.HR_MANAGER, RoleName.PROJECT_MANAGER, RoleName.SITE_ENGINEER));

    private final String title;
    private final String description;
    private final Set<RoleName> roles;

    ReportType(String title, String description, Set<RoleName> roles) {
        this.title = title;
        this.description = description;
        this.roles = roles;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean allows(RoleName role) {
        return roles.contains(role);
    }
}
