package com.sitemaster.settings;

import com.sitemaster.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One editable business value (key/value). Typed access goes through
 * SystemSettingsManager rather than reading rows directly.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (the key is fixed).
 */
@Entity
@Table(name = "system_settings")
public class SystemSetting extends BaseEntity {

    @Column(name = "setting_key", nullable = false, unique = true, length = 100, updatable = false)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 2000)
    private String value;

    @Column(name = "description", length = 255)
    private String description;

    protected SystemSetting() {
        // required by JPA
    }

    public SystemSetting(String key, String value, String description) {
        this.key = key;
        this.value = value;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
    }

    public String getDescription() {
        return description;
    }
}
