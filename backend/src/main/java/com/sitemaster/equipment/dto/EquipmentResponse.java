package com.sitemaster.equipment.dto;

import java.time.LocalDate;

/** An equipment register entry; {@code serviceDue} is true when the next service date has arrived. */
public record EquipmentResponse(Long id, String code, String name, String category, Long projectId,
                                String projectName, String status, String notes, LocalDate lastServiceDate,
                                LocalDate nextServiceDate, boolean serviceDue) {
}
