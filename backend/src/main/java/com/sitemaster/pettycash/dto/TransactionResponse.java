package com.sitemaster.pettycash.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One petty cash movement. */
public record TransactionResponse(Long id, String type, BigDecimal amount, BigDecimal balanceAfter, LocalDate date,
                                  String description, Long expenseId, String recordedByName,
                                  LocalDateTime createdAt) {
}
