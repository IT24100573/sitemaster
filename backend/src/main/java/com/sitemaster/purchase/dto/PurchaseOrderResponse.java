package com.sitemaster.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** A purchase order with its lines and invoice summary. Contains no budget data. */
public record PurchaseOrderResponse(Long id,
                                    String poNumber,
                                    Long materialRequestId,
                                    Long projectId,
                                    String projectName,
                                    String deliveryLocation,
                                    Long supplierId,
                                    String supplierName,
                                    String status,
                                    LocalDate orderDate,
                                    BigDecimal totalAmount,
                                    String notes,
                                    LocalDateTime respondedAt,
                                    String declineReason,
                                    LocalDateTime dispatchedAt,
                                    LocalDate expectedDeliveryDate,
                                    LocalDateTime paidAt,
                                    LocalDateTime deliveredAt,
                                    List<Item> items,
                                    InvoiceSummary invoice) {

    public PurchaseOrderResponse {
        items = List.copyOf(items);
    }

    /** One order line. */
    public record Item(String materialName, String unit, BigDecimal quantity, BigDecimal unitPrice,
                       BigDecimal lineTotal) {
    }

    /** The invoice uploaded for this order, if any. */
    public record InvoiceSummary(Long id, String invoiceNumber, BigDecimal amount, BigDecimal paidAmount,
                                 String status) {
    }
}
