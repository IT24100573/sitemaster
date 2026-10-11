package com.sitemaster.attendance.dto;

import java.util.Map;

/**
 * One employee's month on the attendance sheet.
 *
 * @param employeeId   employee
 * @param employeeNo   employee number
 * @param employeeName name
 * @param days         day of month -&gt; status code (P, A, H, L), lowercase when not yet verified
 * @param present      PRESENT days
 * @param halfDays     HALF_DAY days
 * @param absent       ABSENT days
 * @param onLeave      ON_LEAVE days
 * @param unverified   records still waiting for verification
 */
public record AttendanceSheetRow(Long employeeId, String employeeNo, String employeeName, Map<Integer, String> days,
                                 int present, int halfDays, int absent, int onLeave, int unverified) {

    public AttendanceSheetRow {
        days = Map.copyOf(days);
    }
}
