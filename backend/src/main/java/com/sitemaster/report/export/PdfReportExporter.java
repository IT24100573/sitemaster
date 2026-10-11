package com.sitemaster.report.export;

// PATTERN: Factory Method

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.sitemaster.report.ReportTable;
import com.sitemaster.settings.SystemSettingsManager;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Factory Method pattern - CONCRETE PRODUCT: a landscape A4 PDF table (OpenPDF)
 * with the company name, report title, header row repeated on every page and totals.
 */
public class PdfReportExporter implements ReportExporter {

    private static final Color HEADER_BG = new Color(30, 64, 120);
    private static final Color TOTAL_BG = new Color(241, 245, 249);

    @Override
    public byte[] export(ReportTable table) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);

            document.add(new Paragraph(SystemSettingsManager.getInstance().getSettings().companyName(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph(table.title(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            Paragraph subtitle = new Paragraph(table.subtitle(), FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY));
            subtitle.setSpacingAfter(10);
            document.add(subtitle);

            PdfPTable pdf = new PdfPTable(table.columns().size());
            pdf.setWidthPercentage(100);
            pdf.setHeaderRows(1);
            for (ReportTable.Column column : table.columns()) {
                PdfPCell cell = new PdfPCell(new Phrase(column.header(), white));
                cell.setBackgroundColor(HEADER_BG);
                cell.setPadding(4);
                cell.setHorizontalAlignment(column.numeric() ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                pdf.addCell(cell);
            }
            if (table.rows().isEmpty()) {
                PdfPCell empty = new PdfPCell(new Phrase("No records for this period.", cellFont));
                empty.setColspan(table.columns().size());
                empty.setPadding(6);
                pdf.addCell(empty);
            }
            for (List<Object> row : table.rows()) {
                addRow(pdf, row, table, cellFont, null);
            }
            if (table.totals() != null) {
                addRow(pdf, table.totals(), table, bold, TOTAL_BG);
            }
            document.add(pdf);
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not create the PDF report", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    private static void addRow(PdfPTable pdf, List<Object> values, ReportTable table, Font font, Color background) {
        for (int c = 0; c < values.size(); c++) {
            ReportTable.Column column = table.columns().get(c);
            PdfPCell cell = new PdfPCell(new Phrase(CellFormat.text(values.get(c), column.kind(), true), font));
            cell.setPadding(4);
            cell.setHorizontalAlignment(column.numeric() ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            if (background != null) {
                cell.setBackgroundColor(background);
            }
            pdf.addCell(cell);
        }
    }

    @Override
    public String contentType() {
        return "application/pdf";
    }

    @Override
    public String extension() {
        return "pdf";
    }
}
