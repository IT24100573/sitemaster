package com.sitemaster.report.export;

// PATTERN: Factory Method

import com.sitemaster.report.ReportTable;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Factory Method pattern - CONCRETE PRODUCT: an .xlsx workbook (Apache POI) with
 * real numeric and date cells, so totals can be re-calculated in Excel.
 */
public class ExcelReportExporter implements ReportExporter {

    private static final int MAX_SHEET_NAME = 31;

    @Override
    public byte[] export(ReportTable table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName(table.title()));
            CreationHelper helper = workbook.getCreationHelper();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle bold = workbook.createCellStyle();
            bold.setFont(boldFont);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));
            CellStyle boldMoney = workbook.createCellStyle();
            boldMoney.cloneStyleFrom(money);
            boldMoney.setFont(boldFont);
            CellStyle date = workbook.createCellStyle();
            date.setDataFormat(helper.createDataFormat().getFormat("yyyy-mm-dd"));

            int r = 0;
            Row titleRow = sheet.createRow(r++);
            titleRow.createCell(0).setCellValue(table.title());
            titleRow.getCell(0).setCellStyle(bold);
            sheet.createRow(r++).createCell(0).setCellValue(table.subtitle());
            r++;
            Row header = sheet.createRow(r++);
            for (int c = 0; c < table.columns().size(); c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(table.columns().get(c).header());
                cell.setCellStyle(bold);
            }
            for (List<Object> values : table.rows()) {
                write(sheet.createRow(r++), values, table, money, date, null);
            }
            if (table.totals() != null) {
                write(sheet.createRow(r), table.totals(), table, boldMoney, date, bold);
            }
            for (int c = 0; c < table.columns().size(); c++) {
                sheet.autoSizeColumn(c);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not create the Excel report", ex);
        }
    }

    private static void write(Row row, List<Object> values, ReportTable table, CellStyle money, CellStyle date,
                              CellStyle text) {
        for (int c = 0; c < values.size(); c++) {
            Object value = values.get(c);
            if (value == null) {
                continue;
            }
            Cell cell = row.createCell(c);
            if (value instanceof Number number) {
                cell.setCellValue(new BigDecimal(number.toString()).doubleValue());
                if (table.columns().get(c).kind() == ReportTable.Kind.MONEY) {
                    cell.setCellStyle(money);
                }
            } else if (value instanceof LocalDate localDate) {
                cell.setCellValue(localDate);
                cell.setCellStyle(date);
            } else {
                cell.setCellValue(value.toString());
                if (text != null) {
                    cell.setCellStyle(text);
                }
            }
        }
    }

    private static String sheetName(String title) {
        String clean = title.replaceAll("[\\\\/?*\\[\\]:]", " ");
        return clean.length() > MAX_SHEET_NAME ? clean.substring(0, MAX_SHEET_NAME) : clean;
    }

    @Override
    public String contentType() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    }

    @Override
    public String extension() {
        return "xlsx";
    }
}
