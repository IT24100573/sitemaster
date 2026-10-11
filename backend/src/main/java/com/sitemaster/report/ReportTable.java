package com.sitemaster.report;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A report as a table: what generators produce and exporters render. Cell
 * values are String, Number (BigDecimal/Integer) or LocalDate.
 *
 * @param title    report title
 * @param subtitle period and filters
 * @param columns  column definitions
 * @param rows     data rows
 * @param totals   optional totals row (same width as columns, nulls for blank cells)
 */
public record ReportTable(String title, String subtitle, List<Column> columns, List<List<Object>> rows,
                          List<Object> totals) {

    public ReportTable {
        columns = List.copyOf(columns);
        rows = rows.stream().map(row -> Collections.unmodifiableList(new ArrayList<>(row))).toList();
        totals = totals == null ? null : Collections.unmodifiableList(new ArrayList<>(totals));
    }

    /** How a column is formatted. */
    public enum Kind {
        TEXT,
        NUMBER,
        MONEY,
        PERCENT,
        DATE
    }

    /** One column. */
    public record Column(String header, Kind kind) {

        public boolean numeric() {
            return kind == Kind.NUMBER || kind == Kind.MONEY || kind == Kind.PERCENT;
        }
    }
}
