package com.sitemaster.report.dto;

/** A report the signed-in user may run. */
public record ReportInfo(String type, String title, String description) {
}
