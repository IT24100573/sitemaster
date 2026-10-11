package com.sitemaster.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A phase with its task counts. */
public record PhaseResponse(Long id,
                            String name,
                            String description,
                            LocalDate startDate,
                            LocalDate endDate,
                            BigDecimal budget,
                            String status,
                            int sortOrder,
                            long taskCount,
                            long openTaskCount) {
}
