package com.sitemaster.attendance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One attendance record with its overtime and what the viewer may do. */
public record AttendanceResponse(Long id,
                                 Long employeeId,
                                 String employeeNo,
                                 String employeeName,
                                 String trade,
                                 Long projectId,
                                 String projectName,
                                 Long shiftId,
                                 String shiftName,
                                 LocalDate date,
                                 String status,
                                 LocalDateTime checkIn,
                                 LocalDateTime checkOut,
                                 BigDecimal hoursWorked,
                                 boolean verified,
                                 String verifiedByName,
                                 String markedByName,
                                 String notes,
                                 BigDecimal overtimeHours,
                                 String overtimeStatus,
                                 boolean locked,
                                 boolean canVerify,
                                 boolean canCorrect) {
}
