package com.sitemaster.settings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for system settings. */
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    Optional<SystemSetting> findByKey(String key);
}
