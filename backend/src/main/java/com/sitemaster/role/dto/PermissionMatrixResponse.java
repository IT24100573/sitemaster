package com.sitemaster.role.dto;

import java.util.List;

/**
 * Everything the Roles &amp; Permissions page needs in one call.
 *
 * @param modules     module names in display order
 * @param actions     action names in display order
 * @param permissions every permission (module x action)
 * @param roles       every role with its current grants
 */
public record PermissionMatrixResponse(List<String> modules,
                                       List<String> actions,
                                       List<PermissionResponse> permissions,
                                       List<RoleResponse> roles) {
}
