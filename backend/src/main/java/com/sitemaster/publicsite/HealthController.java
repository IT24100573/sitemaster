package com.sitemaster.publicsite;

import com.sitemaster.user.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Public health check. It runs one cheap query, so "UP" also proves the
 * database connection and Flyway migrations are working.
 */
@RestController
@RequestMapping("/api/public")
public class HealthController {

    private static final String UP = "UP";

    private final UserRepository userRepository;
    private final Clock clock;

    public HealthController(UserRepository userRepository, Clock clock) {
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        userRepository.count();
        return new HealthResponse(UP, clock.getZone().getId(), LocalDateTime.now(clock));
    }
}
