package com.sitemaster.role.dto;

import java.util.List;

/**
 * A role with the ids of the permissions it currently holds.
 *
 * @param id            role id
 * @param name          enum name, e.g. HR_MANAGER
 * @param displayName   human-readable name
 * @param description   what the role does
 * @param userCount     users with this role
 * @param permissionIds granted permission ids
 * @param lockedPermissionIds permissions that cannot be removed from this role
 */
public record RoleResponse(Long id,
                           String name,
                           String displayName,
                           String description,
                           long userCount,
                           List<Long> permissionIds,
                           List<Long> lockedPermissionIds) {

    public RoleResponse {
        permissionIds = List.copyOf(permissionIds);
        lockedPermissionIds = List.copyOf(lockedPermissionIds);
    }
}
