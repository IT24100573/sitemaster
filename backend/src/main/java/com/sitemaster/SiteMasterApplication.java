package com.sitemaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.TimeZone;

/**
 * Entry point of the SiteMaster REST API.
 * <p>
 * The JVM default time zone is set before Spring starts so that every
 * {@link java.time.LocalDateTime} the application creates is in the configured
 * zone (Asia/Colombo unless APP_TIME_ZONE says otherwise).
 * <p>
 * Spring Boot's in-memory default user is excluded because authentication is
 * handled by {@code AuthService} against the users table.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class SiteMasterApplication {

    private static final String TIME_ZONE_ENV = "APP_TIME_ZONE";
    private static final String DEFAULT_TIME_ZONE = "Asia/Colombo";

    public static void main(String[] args) {
        String zone = System.getenv().getOrDefault(TIME_ZONE_ENV, DEFAULT_TIME_ZONE);
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        SpringApplication.run(SiteMasterApplication.class, args);
    }
}
