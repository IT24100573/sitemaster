package com.sitemaster.settings;

// PATTERN: Singleton

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Map;

/**
 * Singleton pattern - one shared, thread-safe cache of the company settings
 * (name, currency, working hours, receipt threshold, rates...) loaded from the
 * system_settings table and reloaded whenever the Admin saves settings.
 * <p>
 * Implementation: classic "initialization-on-demand holder" idiom. The
 * constructor is private, and the JVM guarantees the nested Holder class is
 * initialised exactly once, lazily and thread-safely, on the first call to
 * {@link #getInstance()}. The settings themselves are an immutable snapshot held
 * in a volatile field, so a reload is a single atomic reference swap.
 * <p>
 * Note: Spring beans are already singletons by default (one instance per
 * application context). This class deliberately does NOT rely on that and
 * implements the pattern by hand to show how it works; Spring code reaches it
 * through SystemSettingsLoader.
 */
public final class SystemSettingsManager {

    private volatile CompanySettings settings = CompanySettings.empty();

    private SystemSettingsManager() {
        // private: nobody else can create an instance
    }

    private static final class Holder {
        private static final SystemSettingsManager INSTANCE = new SystemSettingsManager();
    }

    public static SystemSettingsManager getInstance() {
        return Holder.INSTANCE;
    }

    /** Replaces the cached settings with values freshly read from the database. */
    public void reload(Map<String, String> rawValues) {
        this.settings = CompanySettings.fromMap(rawValues);
    }

    public CompanySettings getSettings() {
        return settings;
    }

    public String getCompanyName() {
        return settings.companyName();
    }

    public String getCurrency() {
        return settings.currency();
    }

    public LocalTime getWorkStartTime() {
        return settings.workStartTime();
    }

    public LocalTime getWorkEndTime() {
        return settings.workEndTime();
    }

    public BigDecimal getReceiptThreshold() {
        return settings.receiptThreshold();
    }
}
