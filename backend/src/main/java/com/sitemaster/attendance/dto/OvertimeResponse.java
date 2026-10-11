package com.sitemaster.attendance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Overtime awaiting or after HR's decision. */
public record OvertimeResponse(Long id, Long attendanceId, String employeeName, String employeeNo, String projectName,
                               LocalDate date, BigDecimal hours, String reason, String status, String recordedByName,
                               boolean attendanceVerified, String decidedByName, LocalDateTime decidedAt,
                               String decisionNote) {
}
