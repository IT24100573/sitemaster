package com.sitemaster.settings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Immutable, typed snapshot of all system settings. SystemSettingsManager swaps
 * the whole snapshot atomically on reload, so readers never see half an update.
 * <p>
 * OOP concept: Encapsulation - an immutable value object; parsing is private.
 */
public record CompanySettings(String companyName,
                              String companyDescription,
                              String companyAddress,
                              String companyPhone,
                              String companyEmail,
                              Long companyLogoFileId,
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

    static final String LIST_SEPARATOR = ",";

    public CompanySettings {
        holidays = holidays == null ? List.of() : List.copyOf(holidays);
        trades = trades == null ? List.of() : List.copyOf(trades);
        materialCategories = materialCategories == null ? List.of() : List.copyOf(materialCategories);
        materialUnits = materialUnits == null ? List.of() : List.copyOf(materialUnits);
    }

    /** Snapshot used before the first load. */
    public static CompanySettings empty() {
        return fromMap(Map.of());
    }

    /** Builds a snapshot from raw key/value rows; missing or broken values become empty/zero. */
    public static CompanySettings fromMap(Map<String, String> values) {
        return new CompanySettings(
                text(values, SettingKey.COMPANY_NAME),
                text(values, SettingKey.COMPANY_DESCRIPTION),
                text(values, SettingKey.COMPANY_ADDRESS),
                text(values, SettingKey.COMPANY_PHONE),
                text(values, SettingKey.COMPANY_EMAIL),
                longValue(values, SettingKey.COMPANY_LOGO_FILE_ID),
                text(values, SettingKey.CURRENCY),
                decimal(values, SettingKey.RECEIPT_THRESHOLD),
                time(values, SettingKey.WORK_START_TIME),
                time(values, SettingKey.WORK_END_TIME),
                list(values, SettingKey.HOLIDAYS).stream().map(CompanySettings::parseDate)
                        .filter(java.util.Objects::nonNull).sorted().toList(),
                integer(values, SettingKey.LEAVE_ANNUAL_DAYS),
                integer(values, SettingKey.LEAVE_CASUAL_DAYS),
                integer(values, SettingKey.LEAVE_SICK_DAYS),
                decimal(values, SettingKey.EPF_EMPLOYEE_RATE),
                decimal(values, SettingKey.EPF_EMPLOYER_RATE),
                decimal(values, SettingKey.ETF_RATE),
                decimal(values, SettingKey.OVERTIME_MULTIPLIER),
                decimal(values, SettingKey.SITE_ALLOWANCE_PER_DAY),
                list(values, SettingKey.EMPLOYEE_TRADES),
                list(values, SettingKey.MATERIAL_CATEGORIES),
                list(values, SettingKey.MATERIAL_UNITS));
    }

    public boolean isHoliday(LocalDate date) {
        return holidays.contains(date);
    }

    /** Paid hours in a working day (end minus start of the working day). */
    public BigDecimal workingHoursPerDay() {
        if (workStartTime == null || workEndTime == null || !workEndTime.isAfter(workStartTime)) {
            return BigDecimal.valueOf(8);
        }
        long minutes = Duration.between(workStartTime, workEndTime).toMinutes();
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    /** Monday to Saturday that is not a public holiday. */
    public boolean isWorkingDay(LocalDate date) {
        return date.getDayOfWeek() != DayOfWeek.SUNDAY && !isHoliday(date);
    }

    public boolean hasLogo() {
        return companyLogoFileId != null;
    }

    private static String text(Map<String, String> values, SettingKey key) {
        String value = values.get(key.getKey());
        return value == null ? "" : value.trim();
    }

    private static List<String> list(Map<String, String> values, SettingKey key) {
        String raw = text(values, key);
        if (raw.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(raw.split(LIST_SEPARATOR)).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    private static BigDecimal decimal(Map<String, String> values, SettingKey key) {
        try {
            return new BigDecimal(text(values, key));
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private static int integer(Map<String, String> values, SettingKey key) {
        try {
            return Integer.parseInt(text(values, key));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static Long longValue(Map<String, String> values, SettingKey key) {
        try {
            String raw = text(values, key);
            return raw.isEmpty() ? null : Long.valueOf(raw);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static LocalTime time(Map<String, String> values, SettingKey key) {
        try {
            return LocalTime.parse(text(values, key));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
