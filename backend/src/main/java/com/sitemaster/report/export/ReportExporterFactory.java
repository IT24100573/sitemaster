package com.sitemaster.report.export;

// PATTERN: Factory Method

import com.sitemaster.report.ExportFormat;
import org.springframework.stereotype.Component;

/**
 * Factory Method pattern - the CREATOR. {@link #create(ExportFormat)} decides
 * which concrete exporter to build; the report service only asks for a format.
 * Adding a format means one new ReportExporter class and one line here.
 */
@Component
public class ReportExporterFactory {

    public ReportExporter create(ExportFormat format) {
        return switch (format) {
            case PDF -> new PdfReportExporter();
            case XLSX -> new ExcelReportExporter();
            case CSV -> new CsvReportExporter();
        };
    }
}
