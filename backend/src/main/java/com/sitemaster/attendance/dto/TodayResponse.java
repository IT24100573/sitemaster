package com.sitemaster.attendance.dto;

import com.sitemaster.common.dto.LookupOption;

import java.util.List;

/**
 * The signed-in employee's check-in status for today.
 *
 * @param today     today's record, or null if not checked in
 * @param projects  projects the employee may check in at
 * @param onLeave   true when approved leave covers today
 */
public record TodayResponse(AttendanceResponse today, List<LookupOption> projects, boolean onLeave) {

    public TodayResponse {
        projects = List.copyOf(projects);
    }
}
