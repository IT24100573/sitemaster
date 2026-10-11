package com.sitemaster.role;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.AppModule;
import com.sitemaster.common.enums.PermissionAction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A single grantable permission: one action on one module.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (module and
 * action are immutable; the authority string is derived, never stored).
 */
@Entity
@Table(name = "permissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_permissions_module_action", columnNames = {"module", "action"}))
public class Permission extends BaseEntity {

    private static final String SEPARATOR = ":";

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 40, updatable = false)
    private AppModule module;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20, updatable = false)
    private PermissionAction action;

    @Column(name = "description", length = 255)
    private String description;

    protected Permission() {
        // required by JPA
    }

    public Permission(AppModule module, PermissionAction action, String description) {
        this.module = module;
        this.action = action;
        this.description = description;
    }

    public AppModule getModule() {
        return module;
    }

    public PermissionAction getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** Spring Security authority, e.g. MATERIAL_REQUEST:APPROVE. */
    public String getAuthority() {
        return module.name() + SEPARATOR + action.name();
    }
}
