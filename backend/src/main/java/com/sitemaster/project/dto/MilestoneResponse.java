package com.sitemaster.project.dto;

import java.time.LocalDate;

/** A milestone; {@code overdue} is true when it is past due and not completed. */
public record MilestoneResponse(Long id,
                                String name,
                                LocalDate dueDate,
                                Long phaseId,
                                String phaseName,
                                boolean completed,
                                LocalDate completedDate,
                                boolean overdue) {
}
