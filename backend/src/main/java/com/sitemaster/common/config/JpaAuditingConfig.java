package com.sitemaster.common.config;

import com.sitemaster.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Turns on Spring Data auditing so BaseEntity's createdAt, updatedAt and
 * createdBy are filled automatically. createdBy is the id of the signed-in user,
 * or empty for system actions such as seeding.
 * <p>
 * OOP concept: Abstraction - entities do not know where the current user comes from.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return SecurityUtils::currentUserId;
    }
}
