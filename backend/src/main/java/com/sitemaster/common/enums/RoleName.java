package com.sitemaster.common.enums;

/**
 * The seven SiteMaster roles.
 * <p>
 * OOP concept: Encapsulation - each role carries its own display name, URL path
 * segment and Spring Security authority, so no other class needs to build them.
 */
public enum RoleName {
    ADMIN("Administrator", "admin"),
    PROJECT_MANAGER("Project Manager", "pm"),
    SITE_ENGINEER("Site Engineer", "site"),
    WORKER("Worker", "worker"),
    HR_MANAGER("HR Manager", "hr"),
    ACCOUNTANT("Accountant", "accounts"),
    SUPPLIER("Supplier", "supplier");

    private static final String ROLE_PREFIX = "ROLE_";

    private final String displayName;
    private final String rolePath;

    RoleName(String displayName, String rolePath) {
        this.displayName = displayName;
        this.rolePath = rolePath;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** URL segment used by the frontend (/{rolePath}/dashboard) and the API (/api/{rolePath}/**). */
    public String getRolePath() {
        return rolePath;
    }

    /** Spring Security role authority, e.g. ROLE_ADMIN. */
    public String getAuthority() {
        return ROLE_PREFIX + name();
    }
}
