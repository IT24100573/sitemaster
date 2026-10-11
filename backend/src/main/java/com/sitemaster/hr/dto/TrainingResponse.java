package com.sitemaster.hr.dto;

import java.time.LocalDate;

/** A training record. */
public record TrainingResponse(Long id, Long employeeId, String employeeNo, String employeeName, String title,
                               String provider, boolean safety, LocalDate completedOn, LocalDate expiresOn,
                               boolean expired, String notes, String fileName) {
}
