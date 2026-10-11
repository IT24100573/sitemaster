package com.sitemaster.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.util.unit.DataSize;

import java.time.Duration;

/**
 * Typed view of the "app.*" block in application.yml. Every value can be
 * overridden by an environment variable, so nothing environment-specific is
 * hard-coded in Java.
 * <p>
 * OOP concept: Encapsulation - immutable records expose read-only accessors.
 *
 * @param uploadDir   directory outside the source tree where uploaded files are stored
 * @param frontendUrl base URL of the React app, used to build links sent to users
 * @param security    sign-in and password-reset settings
 * @param files       upload size limits
 * @param seed        demo-data settings used by the DataSeeder
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String uploadDir, String frontendUrl, Security security, Files files, Seed seed) {

    /**
     * @param maxFailedAttempts failed sign-ins allowed before the account is locked
     * @param lockDuration      how long a locked account stays locked
     * @param resetTokenExpiry  how long a password-reset link stays valid
     */
    public record Security(int maxFailedAttempts, Duration lockDuration, Duration resetTokenExpiry) {
    }

    /**
     * @param imageMaxSize    largest photo / profile picture accepted
     * @param documentMaxSize largest document or receipt accepted
     */
    public record Files(DataSize imageMaxSize, DataSize documentMaxSize) {
    }

    /**
     * @param enabled         whether demo accounts are created on an empty database
     * @param defaultPassword password given to every demo account
     */
    public record Seed(boolean enabled, String defaultPassword) {
    }
}
