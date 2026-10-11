package com.sitemaster.leave.dto;

import java.math.BigDecimal;

/** A leave type; {@code entitlement} is the yearly days currently in force. */
public record LeaveTypeResponse(Long id, String code, String name, BigDecimal daysPerYear, BigDecimal entitlement,
                                boolean fromSettings, boolean paid, int backdateDays, boolean active) {
}
