package com.sitemaster.payroll.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** A payslip with every calculation line. */
public record PayslipResponse(Long id, Long payrollRunId, int year, int month, Long employeeId, String employeeNo,
                              String employeeName, String employmentType, BigDecimal grossPay,
                              BigDecimal totalDeductions, BigDecimal netPay, boolean paid, LocalDateTime paidAt,
                              String paymentMethod, String paymentReference, List<Line> lines) {

    public PayslipResponse {
        lines = List.copyOf(lines);
    }

    /** One calculation step. */
    public record Line(String type, String label, BigDecimal amount, BigDecimal runningTotal) {
    }
}
