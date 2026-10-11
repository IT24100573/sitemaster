package com.sitemaster.common.enums;

/**
 * Functional modules that permissions are granted on.
 * A permission authority is written as MODULE:ACTION, e.g. MATERIAL_REQUEST:APPROVE.
 * <p>
 * OOP concept: Abstraction - a closed set of named modules instead of free text.
 */
public enum AppModule {
    USER,
    ROLE,
    EMPLOYEE,
    PROJECT,
    TASK,
    DAILY_REPORT,
    MATERIAL,
    MATERIAL_REQUEST,
    PURCHASE_ORDER,
    SUPPLIER,
    SUPPLIER_INVOICE,
    EQUIPMENT,
    ISSUE,
    DOCUMENT,
    ATTENDANCE,
    LEAVE,
    PAYROLL,
    EXPENSE,
    PAYMENT,
    CLIENT,
    INVOICE,
    REPORT,
    SETTINGS,
    AUDIT_LOG
}
