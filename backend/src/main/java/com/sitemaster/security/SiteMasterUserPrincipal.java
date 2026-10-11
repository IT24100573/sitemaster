package com.sitemaster.security;

import com.sitemaster.common.enums.RoleName;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The signed-in user as stored in the HTTP session. It holds only what the API
 * needs on every request: id, name, role and permission authorities.
 * <p>
 * OOP concept: Encapsulation - immutable, with defensive copies of the
 * permission list; authorities are derived from role + permissions.
 */
public class SiteMasterUserPrincipal implements AuthenticatedPrincipal, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String email;
    private final String fullName;
    private final RoleName role;
    private final List<String> permissions;
    private final int securityVersion;

    public SiteMasterUserPrincipal(Long userId, String email, String fullName,
                                   RoleName role, List<String> permissions, int securityVersion) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.permissions = List.copyOf(permissions);
        this.securityVersion = securityVersion;
    }

    /** ROLE_xxx plus every MODULE:ACTION permission granted to the role. */
    public Collection<GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>(permissions.size() + 1);
        authorities.add(new SimpleGrantedAuthority(role.getAuthority()));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return authorities;
    }

    public boolean hasPermission(String authority) {
        return permissions.contains(authority);
    }

    @Override
    public String getName() {
        return email;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public RoleName getRole() {
        return role;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    /** User's security version when this principal was built; see SessionRefreshFilter. */
    public int getSecurityVersion() {
        return securityVersion;
    }
}
