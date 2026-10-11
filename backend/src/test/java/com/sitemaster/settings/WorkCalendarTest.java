package com.sitemaster.settings;

import com.sitemaster.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

/** Singleton settings feeding the working-day calendar. */
class WorkCalendarTest {

    private final WorkCalendar calendar = new WorkCalendar();

    @BeforeEach
    void setUp() {
        TestData.loadSettings("2026-12-25");
    }

    @Test
    void sundaysAndHolidaysAreNotWorkingDays() {
        assertThat(calendar.isWorkingDay(LocalDate.of(2026, 12, 20))).isFalse(); // Sunday
        assertThat(calendar.isWorkingDay(LocalDate.of(2026, 12, 25))).isFalse(); // holiday
        assertThat(calendar.isWorkingDay(LocalDate.of(2026, 12, 19))).isTrue();  // Saturday is a site day
    }

    @Test
    void countsWorkingDaysInAMonth() {
        // December 2026: 31 days, 4 Sundays, 1 holiday
        assertThat(calendar.workingDays(YearMonth.of(2026, 12))).isEqualTo(26);
    }

    @Test
    void hoursPerDayComeFromTheWorkingTimes() {
        assertThat(calendar.hoursPerDay()).isEqualByComparingTo("9");
    }

    @Test
    void singletonAlwaysReturnsTheSameInstance() {
        assertThat(SystemSettingsManager.getInstance()).isSameAs(SystemSettingsManager.getInstance());
        assertThat(SystemSettingsManager.getInstance().getSettings().currency()).isEqualTo("LKR");
    }
}
