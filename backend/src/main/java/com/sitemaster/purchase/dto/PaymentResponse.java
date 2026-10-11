package com.sitemaster.purchase.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A recorded payment. */
public record PaymentResponse(Long id,
                              Long invoiceId,
                              String invoiceNumber,
                              String poNumber,
                              String supplierName,
                              BigDecimal amount,
                              String method,
                              String referenceNo,
                              LocalDate paymentDate,
                              String notes,
                              String paidByName,
                              LocalDateTime createdAt) {
}
