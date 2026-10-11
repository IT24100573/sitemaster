package com.sitemaster.role;

import com.sitemaster.common.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Grants one Permission to one Role (the editable permission matrix).
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (the pair is
 * immutable; changing a grant means deleting or adding a row).
 */
@Entity
@Table(name = "role_permissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_role_permissions", columnNames = {"role_id", "permission_id"}))
public class RolePermission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, updatable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false, updatable = false)
    private Permission permission;

    protected RolePermission() {
        // required by JPA
    }

    public RolePermission(Role role, Permission permission) {
        this.role = role;
        this.permission = permission;
    }

    public Role getRole() {
        return role;
    }

    public Permission getPermission() {
        return permission;
    }
}
