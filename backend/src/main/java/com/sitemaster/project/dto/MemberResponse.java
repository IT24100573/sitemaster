package com.sitemaster.project.dto;

import java.time.LocalDateTime;

/** A project team member. */
public record MemberResponse(Long userId,
                             String fullName,
                             String email,
                             String phone,
                             String role,
                             String roleDisplayName,
                             boolean hasPhoto,
                             boolean manager,
                             LocalDateTime addedAt) {
}
