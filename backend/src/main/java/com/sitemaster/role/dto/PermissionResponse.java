package com.sitemaster.role.dto;

/**
 * One cell of the permission matrix.
 *
 * @param id        permission id
 * @param module    module, e.g. MATERIAL_REQUEST
 * @param action    action, e.g. APPROVE
 * @param authority MODULE:ACTION
 */
public record PermissionResponse(Long id, String module, String action, String authority) {
}
