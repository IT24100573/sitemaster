package com.sitemaster.role;

import com.sitemaster.role.dto.PermissionMatrixResponse;
import com.sitemaster.role.dto.RolePermissionsUpdateRequest;
import com.sitemaster.role.dto.RoleResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin: Roles &amp; Permissions page. */
@RestController
@RequestMapping("/api/admin/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/matrix")
    @PreAuthorize("hasAuthority('ROLE:VIEW')")
    public PermissionMatrixResponse matrix() {
        return roleService.getMatrix();
    }

    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('ROLE:EDIT')")
    public RoleResponse updatePermissions(@PathVariable Long roleId,
                                          @Valid @RequestBody RolePermissionsUpdateRequest request) {
        return roleService.updatePermissions(roleId, request);
    }
}
