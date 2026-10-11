package com.sitemaster.project.dto;

import java.time.LocalDate;

/** A completed project for the public home page gallery. */
public record PublicProjectCard(Long id,
                                String name,
                                String clientName,
                                String location,
                                LocalDate completedDate,
                                boolean hasCover) {
}
