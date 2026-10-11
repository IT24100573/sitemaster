package com.sitemaster.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** A supplier invoice with its payments. */
public record SupplierInvoiceResponse(Long id,
                                      Long purchaseOrderId,
                                      String poNumber,
                                      String poStatus,
                                      Long supplierId,
                                      String supplierName,
                                      String projectName,
                                      String invoiceNumber,
                                      LocalDate invoiceDate,
                                      BigDecimal amount,
                                      BigDecimal paidAmount,
                                      BigDecimal outstanding,
                                      String status,
                                      String fileName,
                                      boolean payable,
                                      List<PaymentResponse> payments,
                                      LocalDateTime createdAt) {

    public SupplierInvoiceResponse {
        payments = List.copyOf(payments);
    }
}
