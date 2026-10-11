package com.sitemaster.report.export;

// PATTERN: Factory Method

import com.sitemaster.report.ReportTable;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Factory Method pattern - CONCRETE PRODUCT: comma-separated values (UTF-8 with
 * BOM so Excel opens Sinhala/Tamil names correctly). Cells starting with = + - @
 * are prefixed with an apostrophe to prevent formula injection.
 */
public class CsvReportExporter implements ReportExporter {

    private static final String BOM = "\uFEFF";

    @Override
    public byte[] export(ReportTable table) {
        StringBuilder csv = new StringBuilder(BOM);
        csv.append(line(table.columns().stream().map(ReportTable.Column::header).map(h -> (Object) h).toList(), table, true));
        for (List<Object> row : table.rows()) {
            csv.append(line(row, table, false));
        }
        if (table.totals() != null) {
            csv.append(line(table.totals(), table, false));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String line(List<Object> values, ReportTable table, boolean header) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            Object value = values.get(i);
            String text = header ? String.valueOf(value) : CellFormat.text(value, table.columns().get(i).kind(), false);
            sb.append(escape(text, value instanceof Number));
        }
        return sb.append("\r\n").toString();
    }

    private static String escape(String text, boolean number) {
        String safe = text;
        if (!number && !safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
            safe = "'" + safe;
        }
        if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
            safe = "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    @Override
    public String contentType() {
        return "text/csv";
    }

    @Override
    public String extension() {
        return "csv";
    }
}
