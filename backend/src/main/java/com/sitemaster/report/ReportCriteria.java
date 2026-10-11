package com.sitemaster.report;

import java.time.LocalDate;

/**
 * Filters shared by every report.
 *
 * @param from      first day (inclusive)
 * @param to        last day (inclusive)
 * @param projectId optional project
 */
public record ReportCriteria(LocalDate from, LocalDate to, Long projectId) {
}
