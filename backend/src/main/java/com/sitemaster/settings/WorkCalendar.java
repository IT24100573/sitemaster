package com.sitemaster.settings;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Working-day arithmetic from System Settings: Monday-Saturday, excluding the
 * public holidays the Admin lists. Used for leave days and payroll.
 * <p>
 * OOP concept: Encapsulation - callers never read the holiday list themselves.
 */
@Component
public class WorkCalendar {

    public boolean isWorkingDay(LocalDate date) {
        return SystemSettingsManager.getInstance().getSettings().isWorkingDay(date);
    }

    public int workingDaysBetween(LocalDate from, LocalDate to) {
        int count = 0;
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            if (isWorkingDay(day)) {
                count++;
            }
        }
        return count;
    }

    public int workingDays(YearMonth month) {
        return workingDaysBetween(month.atDay(1), month.atEndOfMonth());
    }

    public BigDecimal hoursPerDay() {
        return SystemSettingsManager.getInstance().getSettings().workingHoursPerDay();
    }
}
