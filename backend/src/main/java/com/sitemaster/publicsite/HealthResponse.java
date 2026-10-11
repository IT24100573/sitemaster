package com.sitemaster.publicsite;

import java.time.LocalDateTime;

/**
 * Response of GET /api/public/health.
 *
 * @param status   UP when the API and database answered
 * @param timeZone server time zone
 * @param time     current server time
 */
public record HealthResponse(String status, String timeZone, LocalDateTime time) {
}
