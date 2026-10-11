package com.sitemaster.security;

import com.sitemaster.role.Permission;
import com.sitemaster.role.RolePermissionRepository;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Builds the session principal (role + current permission matrix) for a user.
 * Used at sign-in and whenever a session must be refreshed.
 * <p>
 * OOP concept: Encapsulation - one place decides what goes into a principal.
 */
@Component
public class PrincipalFactory {

    private final UserRepository userRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public PrincipalFactory(UserRepository userRepository, RolePermissionRepository rolePermissionRepository) {
        this.userRepository = userRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public SiteMasterUserPrincipal create(User user) {
        List<String> permissions = rolePermissionRepository.findPermissionsByRoleId(user.getRole().getId())
                .stream()
                .map(Permission::getAuthority)
                .toList();
        return new SiteMasterUserPrincipal(user.getId(), user.getEmail(), user.getFullName(),
                user.getRole().getName(), permissions, user.getSecurityVersion());
    }

    @Transactional(readOnly = true)
    public Optional<SiteMasterUserPrincipal> createFor(Long userId) {
        return userRepository.findById(userId).map(this::create);
    }
}
