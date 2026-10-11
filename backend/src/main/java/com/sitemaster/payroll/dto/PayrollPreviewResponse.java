package com.sitemaster.payroll.dto;

import java.util.List;

/**
 * What HR will send for a month, with anything still waiting that would be left out.
 *
 * @param unverifiedAttendance attendance records not yet verified (not counted)
 * @param pendingOvertime      overtime not yet approved (not paid)
 * @param pendingLeave         leave requests still pending (not counted)
 */
public record PayrollPreviewResponse(int year, int month, int workingDays, boolean alreadySent, List<SummaryRow> rows,
                                     long unverifiedAttendance, long pendingOvertime, long pendingLeave) {

    public PayrollPreviewResponse {
        rows = List.copyOf(rows);
    }
}
