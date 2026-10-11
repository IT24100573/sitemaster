package com.sitemaster.report;

import com.sitemaster.report.dto.ReportInfo;

import java.util.List;

/**
 * Runs reports and exports them through the ReportExporterFactory.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface ReportService {

    List<ReportInfo> available();

    ReportTable preview(ReportType type, ReportCriteria criteria);

    ExportedReport export(ReportType type, ReportCriteria criteria, ExportFormat format);

    /** A rendered report file. */
    record ExportedReport(String fileName, String contentType, byte[] content) {
    }
}
