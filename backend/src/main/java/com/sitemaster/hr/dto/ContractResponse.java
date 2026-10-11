package com.sitemaster.hr.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A contract with its derived status and days left. */
public record ContractResponse(Long id, Long employeeId, String employeeNo, String employeeName, String contractNo,
                               String title, LocalDate startDate, LocalDate endDate, BigDecimal contractValue,
                               String notes, String status, long daysToExpiry, boolean expiringSoon,
                               String fileName) {
}
