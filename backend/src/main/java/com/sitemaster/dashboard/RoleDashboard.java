package com.sitemaster.dashboard;

// PATTERN: Factory Method

import com.sitemaster.common.enums.RoleName;
import com.sitemaster.dashboard.dto.DashboardResponse;
import com.sitemaster.security.SiteMasterUserPrincipal;

/**
 * Factory Method pattern - the PRODUCT interface. Each role has its own
 * dashboard; {@link DashboardFactory} returns the right one, and the
 * controller only calls {@link #build}.
 * <p>
 * OOP concepts: Abstraction and Polymorphism.
 */
public interface RoleDashboard {

    RoleName role();

    DashboardResponse build(SiteMasterUserPrincipal user);
}
