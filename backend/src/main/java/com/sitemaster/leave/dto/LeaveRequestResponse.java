package com.sitemaster.leave.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A leave request. */
public record LeaveRequestResponse(Long id, Long employeeId, String employeeNo, String employeeName,
                                   Long leaveTypeId, String leaveTypeName, LocalDate fromDate, LocalDate toDate,
                                   BigDecimal days, String reason, String status, String decidedByName,
                                   String remarks, LocalDateTime decidedAt, boolean canDecide, boolean canCancel,
                                   LocalDateTime createdAt) {
}
