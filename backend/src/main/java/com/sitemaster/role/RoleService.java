package com.sitemaster.role;

import com.sitemaster.role.dto.PermissionMatrixResponse;
import com.sitemaster.role.dto.RoleResponse;
import com.sitemaster.role.dto.RolePermissionsUpdateRequest;

/**
 * The editable permission matrix.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface RoleService {

    PermissionMatrixResponse getMatrix();

    RoleResponse updatePermissions(Long roleId, RolePermissionsUpdateRequest request);
}
