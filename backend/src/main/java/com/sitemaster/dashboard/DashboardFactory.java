package com.sitemaster.dashboard;

// PATTERN: Factory Method

import com.sitemaster.common.enums.RoleName;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory Method pattern - the CREATOR. {@link #create(RoleName)} returns the
 * dashboard for a role. The concrete dashboards are Spring beans (they need
 * repositories), so the factory receives them all and picks by role; adding a
 * role means one new RoleDashboard class.
 */
@Component
public class DashboardFactory {

    private final Map<RoleName, RoleDashboard> dashboards = new EnumMap<>(RoleName.class);

    public DashboardFactory(List<RoleDashboard> available) {
        available.forEach(d -> dashboards.put(d.role(), d));
    }

    public RoleDashboard create(RoleName role) {
        RoleDashboard dashboard = dashboards.get(role);
        if (dashboard == null) {
            throw new IllegalStateException("No dashboard for role " + role);
        }
        return dashboard;
    }
}
