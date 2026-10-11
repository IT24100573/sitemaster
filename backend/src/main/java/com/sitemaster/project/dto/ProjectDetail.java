package com.sitemaster.project.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything the project overview needs, including what the current user may do.
 */
public record ProjectDetail(Long id,
                            String name,
                            String description,
                            Long clientId,
                            String clientName,
                            Long managerId,
                            String managerName,
                            String location,
                            BigDecimal latitude,
                            BigDecimal longitude,
                            LocalDate startDate,
                            LocalDate endDate,
                            LocalDate completedDate,
                            BigDecimal budget,
                            BigDecimal allocatedBudget,
                            String status,
                            List<String> nextStatuses,
                            int progress,
                            boolean hasCover,
                            int memberCount,
                            long taskCount,
                            long openIssueCount,
                            boolean canManage,
                            boolean isMember) {

    public ProjectDetail {
        nextStatuses = List.copyOf(nextStatuses);
    }
}
