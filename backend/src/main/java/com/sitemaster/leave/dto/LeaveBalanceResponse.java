package com.sitemaster.leave.dto;

import java.math.BigDecimal;

/** One leave type's balance for a year; {@code available} also subtracts pending requests. */
public record LeaveBalanceResponse(Long leaveTypeId, String leaveTypeName, boolean paid, int year,
                                   BigDecimal entitled, BigDecimal used, BigDecimal pending, BigDecimal available) {
}
