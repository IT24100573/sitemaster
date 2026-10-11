package com.sitemaster.report.export;

import com.sitemaster.report.ReportTable;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Text formatting of report cells, shared by the PDF and CSV exporters. */
final class CellFormat {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);

    private CellFormat() {
        // utility class
    }

    static String text(Object value, ReportTable.Kind kind, boolean grouping) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDate date) {
            return date.format(DATE);
        }
        if (value instanceof Number number) {
            BigDecimal decimal = new BigDecimal(number.toString());
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.ENGLISH);
            return switch (kind) {
                case MONEY -> new DecimalFormat(grouping ? "#,##0.00" : "0.00", symbols).format(decimal);
                case PERCENT -> decimal.stripTrailingZeros().toPlainString() + "%";
                default -> decimal.stripTrailingZeros().toPlainString();
            };
        }
        return value.toString();
    }
}
