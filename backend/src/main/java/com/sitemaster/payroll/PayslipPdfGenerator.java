package com.sitemaster.payroll;

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
import com.sitemaster.common.util.EnumLabels;
import com.sitemaster.employee.Employee;
import com.sitemaster.settings.CompanySettings;
import com.sitemaster.settings.SystemSettingsManager;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Renders a payslip as a one-page PDF (OpenPDF): company header, employee
 * details, every calculation line with its running total, and net pay.
 */
@Component
public class PayslipPdfGenerator {

    private static final Color HEADER_BG = new Color(30, 64, 120);
    private static final Color LIGHT_BG = new Color(241, 245, 249);
    private static final DateTimeFormatter PAID_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    public byte[] generate(Payslip payslip) {
        CompanySettings company = SystemSettingsManager.getInstance().getSettings();
        DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        String currency = company.currency() == null ? "" : company.currency() + " ";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            document.add(new Paragraph(company.companyName(), title));
            document.add(new Paragraph(join(company.companyAddress(), company.companyPhone(), company.companyEmail()), small));
            PayrollRun run = payslip.getPayrollRun();
            String period = Month.of(run.getMonth()).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + run.getYear();
            Paragraph heading = new Paragraph("Payslip - " + period, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13));
            heading.setSpacingBefore(14);
            heading.setSpacingAfter(8);
            document.add(heading);

            Employee e = payslip.getEmployee();
            PdfPTable details = new PdfPTable(new float[]{1.2f, 2f, 1.2f, 2f});
            details.setWidthPercentage(100);
            addPair(details, "Employee", e.getUser().getFullName(), bold, normal);
            addPair(details, "Employee no.", e.getEmployeeNo(), bold, normal);
            addPair(details, "Employment", EnumLabels.humanize(payslip.getEmploymentType()), bold, normal);
            addPair(details, "Trade", e.getTrade() == null ? "-" : e.getTrade(), bold, normal);
            addPair(details, "Status", payslip.isPaid() ? "Paid " + payslip.getPaidAt().format(PAID_FORMAT) : "Not yet paid",
                    bold, normal);
            addPair(details, "Payment", payslip.getPaymentMethod() == null ? "-"
                    : EnumLabels.humanize(payslip.getPaymentMethod())
                    + (payslip.getPaymentReference() == null ? "" : " (" + payslip.getPaymentReference() + ")"), bold, normal);
            details.setSpacingAfter(14);
            document.add(details);

            PdfPTable lines = new PdfPTable(new float[]{4f, 1.4f, 1.6f});
            lines.setWidthPercentage(100);
            for (String header : new String[]{"Item", "Amount", "Running total"}) {
                PdfPCell cell = new PdfPCell(new Phrase(header, white));
                cell.setBackgroundColor(HEADER_BG);
                cell.setPadding(6);
                cell.setHorizontalAlignment("Item".equals(header) ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
                lines.addCell(cell);
            }
            for (PayslipLine line : payslip.getLines()) {
                lines.addCell(cell(line.getLabel(), normal, Element.ALIGN_LEFT, null));
                lines.addCell(cell(money.format(line.getAmount()), normal, Element.ALIGN_RIGHT, null));
                lines.addCell(cell(money.format(line.getRunningTotal()), normal, Element.ALIGN_RIGHT, null));
            }
            addTotal(lines, "Gross pay", payslip.getGrossPay(), currency, money, bold);
            addTotal(lines, "Total deductions", payslip.getTotalDeductions(), currency, money, bold);
            addTotal(lines, "Net pay", payslip.getNetPay(), currency, money, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12));
            document.add(lines);

            Paragraph note = new Paragraph("This payslip was generated by SiteMaster. Overtime and attendance are "
                    + "counted only when verified and approved.", small);
            note.setSpacingBefore(18);
            document.add(note);
        } catch (DocumentException ex) {
            throw new IllegalStateException("Could not create the payslip PDF", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    private static void addPair(PdfPTable table, String label, String value, Font bold, Font normal) {
        table.addCell(cell(label, bold, Element.ALIGN_LEFT, LIGHT_BG));
        table.addCell(cell(value, normal, Element.ALIGN_LEFT, null));
    }

    private static void addTotal(PdfPTable table, String label, BigDecimal amount, String currency, DecimalFormat money,
                                 Font font) {
        PdfPCell labelCell = cell(label, font, Element.ALIGN_RIGHT, LIGHT_BG);
        labelCell.setColspan(2);
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

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append("  |  ");
                }
                sb.append(part);
            }
        }
        return sb.toString();
    }
}
