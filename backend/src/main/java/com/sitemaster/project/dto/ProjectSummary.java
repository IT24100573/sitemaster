package com.sitemaster.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row of the project list. */
public record ProjectSummary(Long id,
                             String name,
                             String clientName,
                             String managerName,
                             String location,
                             String status,
                             int progress,
                             LocalDate startDate,
                             LocalDate endDate,
                             BigDecimal budget,
                             boolean hasCover) {
}
