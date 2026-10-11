package com.sitemaster.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A payroll run with its totals. */
public record PayrollRunResponse(Long id, int year, int month, String status, int workingDays,
                                 String summarySentByName, LocalDateTime summarySentAt, String calculatedByName,
                                 LocalDateTime calculatedAt, LocalDateTime paidAt, BigDecimal totalGross,
                                 BigDecimal totalDeductions, BigDecimal totalNet, int employees, long unpaidPayslips) {
}
