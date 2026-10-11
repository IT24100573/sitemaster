package com.sitemaster.role.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request body of PUT /api/admin/roles/{id}/permissions: the complete new set of
 * permission ids for the role (it replaces the old set).
 * <p>
 * OOP concept: Encapsulation - private field with explicit accessors.
 */
public class RolePermissionsUpdateRequest {

    @NotNull(message = "{field.required}")
    @Size(max = 500, message = "{permissions.size}")
    private List<@NotNull(message = "{field.required}") Long> permissionIds;

    public List<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }
}
