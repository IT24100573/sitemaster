package com.sitemaster.workflow;

import com.sitemaster.common.enums.AppModule;

/**
 * Every hand-off or notable action that observers react to. Each type knows the
 * module and entity it belongs to; the wording lives in messages.properties under
 * event.TYPE.audit / event.TYPE.title / event.TYPE.message, where {0} is always
 * the actor's name and {1}.. are the event arguments.
 * <p>
 * OOP concept: Encapsulation - module and entity type travel with the event type.
 */
public enum WorkflowEventType {
    USER_LOGGED_IN(AppModule.USER, "User"),
    USER_CREATED(AppModule.USER, "User"),
    USER_UPDATED(AppModule.USER, "User"),
    USER_ROLE_CHANGED(AppModule.USER, "User"),
    USER_ACTIVATED(AppModule.USER, "User"),
    USER_DEACTIVATED(AppModule.USER, "User"),
    USER_UNLOCKED(AppModule.USER, "User"),
    PASSWORD_LINK_SENT(AppModule.USER, "User"),
    PASSWORD_CHANGED(AppModule.USER, "User"),
    PROFILE_UPDATED(AppModule.USER, "User"),
    ROLE_PERMISSIONS_UPDATED(AppModule.ROLE, "Role"),
    SETTINGS_UPDATED(AppModule.SETTINGS, "SystemSetting"),
    COMPANY_LOGO_UPDATED(AppModule.SETTINGS, "SystemSetting"),
    EMPLOYEE_CREATED(AppModule.EMPLOYEE, "Employee"),
    EMPLOYEE_UPDATED(AppModule.EMPLOYEE, "Employee"),
    EMPLOYEE_DOCUMENT_ADDED(AppModule.EMPLOYEE, "Employee"),
    EMPLOYEE_DOCUMENT_REMOVED(AppModule.EMPLOYEE, "Employee"),
    CLIENT_CREATED(AppModule.CLIENT, "Client"),
    CLIENT_UPDATED(AppModule.CLIENT, "Client"),
    PROJECT_CREATED(AppModule.PROJECT, "Project"),
    PROJECT_UPDATED(AppModule.PROJECT, "Project"),
    PROJECT_MANAGER_ASSIGNED(AppModule.PROJECT, "Project"),
    PROJECT_STATUS_CHANGED(AppModule.PROJECT, "Project"),
    PROJECT_MEMBER_ADDED(AppModule.PROJECT, "Project"),
    PROJECT_MEMBER_REMOVED(AppModule.PROJECT, "Project"),
    PHASE_SAVED(AppModule.PROJECT, "Phase"),
    PHASE_STATUS_CHANGED(AppModule.PROJECT, "Phase"),
    PHASE_DELETED(AppModule.PROJECT, "Phase"),
    MILESTONE_SAVED(AppModule.PROJECT, "Milestone"),
    MILESTONE_COMPLETED(AppModule.PROJECT, "Milestone"),
    MILESTONE_DELETED(AppModule.PROJECT, "Milestone"),
    TASK_ASSIGNED(AppModule.TASK, "Task"),
    TASK_UPDATED(AppModule.TASK, "Task"),
    TASK_COMPLETED(AppModule.TASK, "Task"),
    TASK_DELETED(AppModule.TASK, "Task"),
    SUBTASK_ASSIGNED(AppModule.TASK, "SubTask"),
    SUBTASK_COMPLETED(AppModule.TASK, "SubTask"),
    SUBTASK_DELETED(AppModule.TASK, "SubTask"),
    DOCUMENT_UPLOADED(AppModule.DOCUMENT, "ProjectDocument"),
    DOCUMENT_VERSION_ADDED(AppModule.DOCUMENT, "ProjectDocument"),
    DOCUMENT_DELETED(AppModule.DOCUMENT, "ProjectDocument"),
    ISSUE_REPORTED(AppModule.ISSUE, "Issue"),
    ISSUE_UPDATED(AppModule.ISSUE, "Issue"),
    ISSUE_STATUS_CHANGED(AppModule.ISSUE, "Issue"),
    SUPPLIER_APPLICATION_SUBMITTED(AppModule.SUPPLIER, "SupplierApplication"),
    SUPPLIER_APPLICATION_APPROVED(AppModule.SUPPLIER, "SupplierApplication"),
    SUPPLIER_APPLICATION_REJECTED(AppModule.SUPPLIER, "SupplierApplication"),
    SUPPLIER_STATUS_CHANGED(AppModule.SUPPLIER, "Supplier"),
    SUPPLIER_PROFILE_UPDATED(AppModule.SUPPLIER, "Supplier"),
    MATERIAL_SAVED(AppModule.MATERIAL, "Material"),
    EQUIPMENT_SAVED(AppModule.EQUIPMENT, "Equipment"),
    EQUIPMENT_ASSIGNED(AppModule.EQUIPMENT, "Equipment"),
    EQUIPMENT_STATUS_CHANGED(AppModule.EQUIPMENT, "Equipment"),
    DAILY_REPORT_SUBMITTED(AppModule.DAILY_REPORT, "DailyReport"),
    DAILY_REPORT_APPROVED(AppModule.DAILY_REPORT, "DailyReport"),
    DAILY_REPORT_REJECTED(AppModule.DAILY_REPORT, "DailyReport"),
    MATERIAL_REQUEST_CREATED(AppModule.MATERIAL_REQUEST, "MaterialRequest"),
    MATERIAL_REQUEST_APPROVED(AppModule.MATERIAL_REQUEST, "MaterialRequest"),
    MATERIAL_REQUEST_REJECTED(AppModule.MATERIAL_REQUEST, "MaterialRequest"),
    MATERIAL_DELIVERED(AppModule.MATERIAL_REQUEST, "MaterialRequest"),
    PURCHASE_ORDER_SENT(AppModule.PURCHASE_ORDER, "PurchaseOrder"),
    PURCHASE_ORDER_ACCEPTED(AppModule.PURCHASE_ORDER, "PurchaseOrder"),
    PURCHASE_ORDER_DECLINED(AppModule.PURCHASE_ORDER, "PurchaseOrder"),
    PURCHASE_ORDER_DISPATCHED(AppModule.PURCHASE_ORDER, "PurchaseOrder"),
    SUPPLIER_INVOICE_SUBMITTED(AppModule.SUPPLIER_INVOICE, "SupplierInvoice"),
    SUPPLIER_INVOICE_PAID(AppModule.PAYMENT, "Payment"),
    SHIFT_SAVED(AppModule.ATTENDANCE, "Shift"),
    ATTENDANCE_CHECKED_IN(AppModule.ATTENDANCE, "Attendance"),
    ATTENDANCE_CHECKED_OUT(AppModule.ATTENDANCE, "Attendance"),
    ATTENDANCE_MARKED(AppModule.ATTENDANCE, "Attendance"),
    ATTENDANCE_CORRECTED(AppModule.ATTENDANCE, "Attendance"),
    ATTENDANCE_VERIFIED(AppModule.ATTENDANCE, "Attendance"),
    OVERTIME_RECORDED(AppModule.ATTENDANCE, "Overtime"),
    OVERTIME_APPROVED(AppModule.ATTENDANCE, "Overtime"),
    OVERTIME_REJECTED(AppModule.ATTENDANCE, "Overtime"),
    LEAVE_TYPE_SAVED(AppModule.LEAVE, "LeaveType"),
    LEAVE_REQUESTED(AppModule.LEAVE, "LeaveRequest"),
    LEAVE_APPROVED(AppModule.LEAVE, "LeaveRequest"),
    LEAVE_REJECTED(AppModule.LEAVE, "LeaveRequest"),
    LEAVE_CANCELLED(AppModule.LEAVE, "LeaveRequest"),
    CONTRACT_SAVED(AppModule.EMPLOYEE, "Contract"),
    CONTRACT_EXPIRING(AppModule.EMPLOYEE, "Contract"),
    TRAINING_SAVED(AppModule.EMPLOYEE, "TrainingRecord"),
    TRAINING_DELETED(AppModule.EMPLOYEE, "TrainingRecord"),
    PAYROLL_SUMMARY_SENT(AppModule.PAYROLL, "PayrollRun"),
    PAYROLL_CALCULATED(AppModule.PAYROLL, "PayrollRun"),
    PAYSLIP_PAID(AppModule.PAYROLL, "Payslip"),
    PAYROLL_PAID(AppModule.PAYROLL, "PayrollRun"),
    EXPENSE_SUBMITTED(AppModule.EXPENSE, "ExpenseRequest"),
    EXPENSE_APPROVED(AppModule.EXPENSE, "ExpenseRequest"),
    EXPENSE_REJECTED(AppModule.EXPENSE, "ExpenseRequest"),
    EXPENSE_PAID(AppModule.PAYMENT, "ExpenseRequest"),
    PETTY_CASH_FUND_SAVED(AppModule.PAYMENT, "PettyCashFund"),
    PETTY_CASH_TOPPED_UP(AppModule.PAYMENT, "PettyCashFund"),
    PETTY_CASH_ADJUSTED(AppModule.PAYMENT, "PettyCashFund"),
    CLIENT_INVOICE_SAVED(AppModule.INVOICE, "ClientInvoice"),
    CLIENT_INVOICE_ISSUED(AppModule.INVOICE, "ClientInvoice"),
    CLIENT_INVOICE_DELETED(AppModule.INVOICE, "ClientInvoice"),
    CLIENT_PAYMENT_RECEIVED(AppModule.INVOICE, "ClientInvoice"),
    REPORT_EXPORTED(AppModule.REPORT, "Report");

    private final AppModule module;
    private final String entityType;

    WorkflowEventType(AppModule module, String entityType) {
        this.module = module;
        this.entityType = entityType;
    }

    public AppModule getModule() {
        return module;
    }

    public String getEntityType() {
        return entityType;
    }

    public String auditKey() {
        return "event." + name() + ".audit";
    }

    public String titleKey() {
        return "event." + name() + ".title";
    }

    public String messageKey() {
        return "event." + name() + ".message";
    }
}
