package com.sitemaster.clientinvoice;

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
import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.common.util.EnumLabels;
import com.sitemaster.settings.CompanySettings;
import com.sitemaster.settings.SystemSettingsManager;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Renders a client invoice as a one-page PDF (OpenPDF). */
@Component
public class InvoicePdfGenerator {

    private static final Color HEADER_BG = new Color(30, 64, 120);
    private static final Color LIGHT_BG = new Color(241, 245, 249);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    public byte[] generate(ClientInvoice invoice, InvoiceStatus displayStatus) {
        CompanySettings company = SystemSettingsManager.getInstance().getSettings();
        DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        String currency = company.currency() == null ? "" : company.currency() + " ";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            document.add(new Paragraph(company.companyName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph(String.join("  |  ", nonBlank(company.companyAddress()),
                    nonBlank(company.companyPhone()), nonBlank(company.companyEmail())), small));

            Paragraph title = new Paragraph("Invoice " + invoice.getInvoiceNumber(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13));
            title.setSpacingBefore(14);
            title.setSpacingAfter(8);
            document.add(title);

            PdfPTable head = new PdfPTable(new float[]{1.2f, 2f, 1.2f, 2f});
            head.setWidthPercentage(100);
            pair(head, "Bill to", invoice.getClient().getName(), bold, normal);
            pair(head, "Invoice date", invoice.getIssueDate().format(DATE), bold, normal);
            pair(head, "Address", nonBlank(invoice.getClient().getAddress()), bold, normal);
            pair(head, "Due date", invoice.getDueDate().format(DATE), bold, normal);
            pair(head, "Project", invoice.getProject().getName(), bold, normal);
            pair(head, "Status", EnumLabels.humanize(displayStatus), bold, normal);
            head.setSpacingAfter(14);
            document.add(head);

            PdfPTable lines = new PdfPTable(new float[]{4f, 1.2f, 1.6f, 1.6f});
            lines.setWidthPercentage(100);
            for (String h : new String[]{"Description", "Quantity", "Unit price", "Amount"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, white));
                cell.setBackgroundColor(HEADER_BG);
                cell.setPadding(6);
                cell.setHorizontalAlignment("Description".equals(h) ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
                lines.addCell(cell);
            }
            for (ClientInvoiceLine line : invoice.getLines()) {
                lines.addCell(cell(line.getDescription(), normal, Element.ALIGN_LEFT, null));
                lines.addCell(cell(line.getQuantity().stripTrailingZeros().toPlainString(), normal, Element.ALIGN_RIGHT, null));
                lines.addCell(cell(money.format(line.getUnitPrice()), normal, Element.ALIGN_RIGHT, null));
                lines.addCell(cell(money.format(line.getLineTotal()), normal, Element.ALIGN_RIGHT, null));
            }
            total(lines, "Subtotal", invoice.getSubtotal(), currency, money, bold);
            total(lines, "Tax (" + invoice.getTaxRate().stripTrailingZeros().toPlainString() + "%)",
                    invoice.getTaxAmount(), currency, money, bold);
            total(lines, "Total", invoice.getTotal(), currency, money, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            if (invoice.getPaidAmount().signum() > 0) {
                total(lines, "Paid", invoice.getPaidAmount(), currency, money, bold);
                total(lines, "Balance due", invoice.outstanding(), currency, money, bold);
            }
            document.add(lines);

            if (invoice.getNotes() != null) {
                Paragraph notes = new Paragraph("Notes: " + invoice.getNotes(), normal);
                notes.setSpacingBefore(14);
                document.add(notes);
            }
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not create the invoice PDF", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static void pair(PdfPTable table, String label, String value, Font bold, Font normal) {
        table.addCell(cell(label, bold, Element.ALIGN_LEFT, LIGHT_BG));
        table.addCell(cell(value, normal, Element.ALIGN_LEFT, null));
    }

    private static void total(PdfPTable table, String label, BigDecimal amount, String currency, DecimalFormat money,
                              Font font) {
        PdfPCell labelCell = cell(label, font, Element.ALIGN_RIGHT, LIGHT_BG);
        labelCell.setColspan(3);
        table.addCell(labelCell);
        table.addCell(cell(currency + money.format(amount), font, Element.ALIGN_RIGHT, LIGHT_BG));
    }

    private static PdfPCell cell(String text, Font font, int align, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setPadding(5);
        cell.setHorizontalAlignment(align);
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        return cell;
    }
}
