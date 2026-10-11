package com.sitemaster.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Exposes a {@link Clock} bean so services never call LocalDateTime.now()
 * directly; tests can replace it with a fixed clock.
 * <p>
 * OOP concept: Abstraction - time is a dependency, not a static call.
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
