package com.sitemaster.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** An expense request with what the viewer may do next. */
public record ExpenseResponse(Long id,
                              Long projectId,
                              String projectName,
                              Long phaseId,
                              String phaseName,
                              String category,
                              BigDecimal amount,
                              LocalDate expenseDate,
                              String payee,
                              String description,
                              String receiptName,
                              boolean receiptRequired,
                              String status,
                              Long requestedById,
                              String requestedByName,
                              String approvedByName,
                              String remarks,
                              LocalDateTime decidedAt,
                              LocalDateTime paidAt,
                              String paymentMethod,
                              String paymentReference,
                              boolean paidFromPettyCash,
                              boolean canReview,
                              boolean canPay,
                              LocalDateTime createdAt) {
}
