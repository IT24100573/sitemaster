package com.sitemaster.attendance.dto;

import java.time.LocalTime;

/** A shift. */
public record ShiftResponse(Long id, String name, LocalTime startTime, LocalTime endTime, String description,
                            boolean active) {
}
