package com.sitemaster.settings.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * All editable settings, as shown on the System Settings page.
 */
public record SystemSettingsResponse(String companyName,
                                     String companyDescription,
                                     String companyAddress,
                                     String companyPhone,
                                     String companyEmail,
                                     boolean hasLogo,
                                     String currency,
                                     BigDecimal receiptThreshold,
                                     LocalTime workStartTime,
                                     LocalTime workEndTime,
                                     List<LocalDate> holidays,
                                     int leaveAnnualDays,
                                     int leaveCasualDays,
                                     int leaveSickDays,
                                     BigDecimal epfEmployeeRate,
                                     BigDecimal epfEmployerRate,
                                     BigDecimal etfRate,
                                     BigDecimal overtimeMultiplier,
                                     BigDecimal siteAllowancePerDay,
                                     List<String> trades,
                                     List<String> materialCategories,
                                     List<String> materialUnits) {
}
