package com.sitemaster.role;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.RoleName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * One of the seven system roles. Roles are reference data created by Flyway;
 * their permissions are edited by the Admin through RolePermission rows.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (the role
 * name is fixed after creation, so it has no setter).
 */
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "name", nullable = false, unique = true, length = 30, updatable = false)
    private RoleName name;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "description", length = 255)
    private String description;

    protected Role() {
        // required by JPA
    }

    public Role(RoleName name, String displayName, String description) {
        this.name = name;
        this.displayName = displayName;
        this.description = description;
    }

    public RoleName getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
