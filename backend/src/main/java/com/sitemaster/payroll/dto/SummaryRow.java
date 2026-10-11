package com.sitemaster.payroll.dto;

import java.math.BigDecimal;

/** One employee's attendance figures for a payroll month. */
public record SummaryRow(Long employeeId, String employeeNo, String employeeName, String employmentType,
                         int presentDays, int halfDays, int absentDays, BigDecimal paidLeaveDays,
                         BigDecimal unpaidLeaveDays, BigDecimal overtimeHours) {
}
