package com.sitemaster.role;

import com.sitemaster.common.enums.AppModule;
import com.sitemaster.common.enums.PermissionAction;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.role.dto.PermissionMatrixResponse;
import com.sitemaster.role.dto.PermissionResponse;
import com.sitemaster.role.dto.RolePermissionsUpdateRequest;
import com.sitemaster.role.dto.RoleResponse;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link RoleService}.
 * Rule: the ADMIN role always keeps ROLE:VIEW and ROLE:EDIT, so an administrator
 * can never lock everyone out of this page. After a change every user with that
 * role has their session permissions refreshed on the next request.
 * <p>
 * OOP concept: Abstraction - implements RoleService.
 */
@Service
public class RoleServiceImpl implements RoleService {

    private static final Set<String> ADMIN_LOCKED = Set.of("ROLE:VIEW", "ROLE:EDIT");

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;

    public RoleServiceImpl(RoleRepository roleRepository, PermissionRepository permissionRepository,
                           RolePermissionRepository rolePermissionRepository, UserRepository userRepository,
                           CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.userRepository = userRepository;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public PermissionMatrixResponse getMatrix() {
        List<Permission> permissions = permissionRepository.findAll().stream()
                .sorted(Comparator.comparing((Permission p) -> p.getModule().ordinal())
                        .thenComparing(p -> p.getAction().ordinal()))
                .toList();
        Map<Long, List<Long>> grants = new HashMap<>();
        for (Object[] pair : rolePermissionRepository.findAllRolePermissionPairs()) {
            grants.computeIfAbsent((Long) pair[0], k -> new ArrayList<>()).add((Long) pair[1]);
        }
        List<RoleResponse> roles = roleRepository.findAll(Sort.by("id")).stream()
                .map(role -> toResponse(role, grants.getOrDefault(role.getId(), List.of()), permissions))
                .toList();
        return new PermissionMatrixResponse(
                Arrays.stream(AppModule.values()).map(Enum::name).toList(),
                Arrays.stream(PermissionAction.values()).map(Enum::name).toList(),
                permissions.stream().map(RoleServiceImpl::toPermissionResponse).toList(),
                roles);
    }

    @Override
    @Transactional
    public RoleResponse updatePermissions(Long roleId, RolePermissionsUpdateRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Role"));
        Set<Long> requestedIds = new LinkedHashSet<>(request.getPermissionIds());
        List<Permission> permissions = permissionRepository.findAllById(requestedIds);
        if (permissions.size() != requestedIds.size()) {
            throw BusinessRuleException.forField("permissionIds", "permissions.unknown");
        }
        if (role.getName() == RoleName.ADMIN) {
            Set<String> granted = new HashSet<>();
            permissions.forEach(p -> granted.add(p.getAuthority()));
            if (!granted.containsAll(ADMIN_LOCKED)) {
                throw BusinessRuleException.forField("permissionIds", "permissions.adminLocked");
            }
        }

        rolePermissionRepository.deleteByRoleId(role.getId());
        rolePermissionRepository.saveAll(permissions.stream().map(p -> new RolePermission(role, p)).toList());
        userRepository.bumpSecurityVersionForRole(role.getId());

        SiteMasterUserPrincipal actor = currentUser.requirePrincipal();
        eventPublisher.publish(WorkflowEvent.builder(WorkflowEventType.ROLE_PERMISSIONS_UPDATED)
                .actor(actor.getUserId(), actor.getFullName())
                .entityId(role.getId())
                .args(role.getDisplayName(), permissions.size())
                .build());

        return toResponse(role, permissions.stream().map(Permission::getId).toList(),
                permissionRepository.findAll());
    }

    private RoleResponse toResponse(Role role, List<Long> grantedIds, List<Permission> allPermissions) {
        List<Long> locked = role.getName() == RoleName.ADMIN
                ? allPermissions.stream().filter(p -> ADMIN_LOCKED.contains(p.getAuthority())).map(Permission::getId).toList()
                : List.of();
        return new RoleResponse(role.getId(), role.getName().name(), role.getDisplayName(), role.getDescription(),
                userRepository.countByRoleId(role.getId()), grantedIds.stream().sorted().toList(), locked);
    }

    private static PermissionResponse toPermissionResponse(Permission p) {
        return new PermissionResponse(p.getId(), p.getModule().name(), p.getAction().name(), p.getAuthority());
    }
}
