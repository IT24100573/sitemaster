package com.sitemaster.clientinvoice.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** A client invoice with lines and receipts; {@code status} may be OVERDUE (derived). */
public record ClientInvoiceResponse(Long id, String invoiceNumber, Long projectId, String projectName, Long clientId,
                                    String clientName, String clientAddress, LocalDate issueDate, LocalDate dueDate,
                                    String status, BigDecimal taxRate, BigDecimal subtotal, BigDecimal taxAmount,
                                    BigDecimal total, BigDecimal paidAmount, BigDecimal outstanding, String notes,
                                    LocalDateTime issuedAt, List<Line> lines, List<Receipt> receipts) {

    public ClientInvoiceResponse {
        lines = List.copyOf(lines);
        receipts = List.copyOf(receipts);
    }

    /** One billed line. */
    public record Line(String description, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    /** One receipt. */
    public record Receipt(Long id, BigDecimal amount, LocalDate paymentDate, String method, String referenceNo,
                          String notes, String receivedByName) {
    }
}
