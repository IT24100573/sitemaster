package com.sitemaster.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Turns on scheduled jobs (contract renewal reminders). Set
 * {@code sitemaster.scheduling.enabled=false} to switch them off, e.g. in tests.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "sitemaster.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
