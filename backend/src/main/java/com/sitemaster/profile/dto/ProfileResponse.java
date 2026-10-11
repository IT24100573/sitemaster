package com.sitemaster.profile.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * The signed-in user's own profile, with employment details when they have an
 * employee record (suppliers do not).
 */
public record ProfileResponse(Long id,
                              String fullName,
                              String email,
                              String phone,
                              String role,
                              String roleDisplayName,
                              boolean hasPhoto,
                              LocalDateTime lastLoginAt,
                              Employment employment) {

    /**
     * Read-only employment summary.
     *
     * @param employeeNo     employee number
     * @param employmentType PERMANENT, CONTRACT or DAILY_WAGE
     * @param trade          trade (workers)
     * @param joinDate       start date
     */
    public record Employment(String employeeNo, String employmentType, String trade, LocalDate joinDate) {
    }
}
