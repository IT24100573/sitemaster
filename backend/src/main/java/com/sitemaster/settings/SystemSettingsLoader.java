package com.sitemaster.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bridge between Spring and the hand-written SystemSettingsManager singleton:
 * loads the settings at start-up and after every save. Loading on
 * ApplicationStartedEvent (before any CommandLineRunner) means the demo data
 * seeder already sees the real trades, categories, rates and holidays.
 */
@Component
public class SystemSettingsLoader {

    private static final Logger log = LoggerFactory.getLogger(SystemSettingsLoader.class);

    private final SystemSettingRepository repository;

    public SystemSettingsLoader(SystemSettingRepository repository) {
        this.repository = repository;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void loadOnStartup() {
        reload();
        log.info("System settings loaded for {}", SystemSettingsManager.getInstance().getCompanyName());
    }

    @Transactional(readOnly = true)
    public void reload() {
        Map<String, String> values = repository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getKey, SystemSetting::getValue));
        SystemSettingsManager.getInstance().reload(values);
    }
}
