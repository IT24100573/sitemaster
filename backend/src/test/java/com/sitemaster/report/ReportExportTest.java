package com.sitemaster.report;

import com.sitemaster.report.export.CsvReportExporter;
import com.sitemaster.report.export.ExcelReportExporter;
import com.sitemaster.report.export.PdfReportExporter;
import com.sitemaster.report.export.ReportExporter;
import com.sitemaster.report.export.ReportExporterFactory;
import com.sitemaster.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Factory Method: one ReportTable, three output formats. */
class ReportExportTest {

    private final ReportExporterFactory factory = new ReportExporterFactory();

    private final ReportTable table = new ReportTable("Expenses", "Sep 2026",
            List.of(new ReportTable.Column("Description", ReportTable.Kind.TEXT),
                    new ReportTable.Column("Payee", ReportTable.Kind.TEXT)),
            List.of(List.<Object>of("Diesel, 40 litres", "Ceypetco"),
                    List.<Object>of("=HYPERLINK(\"x\")", "Say \"hi\"")),
            null);

    @BeforeEach
    void settings() {
        TestData.loadSettings();
    }

    @Test
    void factoryCreatesTheRightExporter() {
        assertThat(factory.create(ExportFormat.PDF)).isInstanceOf(PdfReportExporter.class);
        assertThat(factory.create(ExportFormat.XLSX)).isInstanceOf(ExcelReportExporter.class);
        assertThat(factory.create(ExportFormat.CSV)).isInstanceOf(CsvReportExporter.class);
    }

    @Test
    void csvQuotesCommasAndQuotesAndDefusesFormulas() {
        String csv = new String(factory.create(ExportFormat.CSV).export(table), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("\uFEFFDescription,Payee\r\n");
        assertThat(csv).contains("\"Diesel, 40 litres\",Ceypetco\r\n");
        assertThat(csv).contains("\"'=HYPERLINK(\"\"x\"\")\",\"Say \"\"hi\"\"\"");
    }

    @Test
    void pdfAndExcelProduceRealFiles() {
        ReportExporter pdf = factory.create(ExportFormat.PDF);
        ReportExporter xlsx = factory.create(ExportFormat.XLSX);
        assertThat(new String(pdf.export(table), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        byte[] excel = xlsx.export(table);
        assertThat(excel[0]).isEqualTo((byte) 'P');
        assertThat(excel[1]).isEqualTo((byte) 'K');   // xlsx is a zip archive
    }
}
