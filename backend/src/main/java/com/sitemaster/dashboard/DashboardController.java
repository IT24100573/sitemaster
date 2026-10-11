package com.sitemaster.dashboard;

import com.sitemaster.dashboard.dto.DashboardResponse;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/dashboard returns the signed-in user's role dashboard, built by the
 * dashboard the {@link DashboardFactory} creates for that role.
 */
@RestController
public class DashboardController {

    private final DashboardFactory factory;
    private final CurrentUserProvider currentUser;

    public DashboardController(DashboardFactory factory, CurrentUserProvider currentUser) {
        this.factory = factory;
        this.currentUser = currentUser;
    }

    @GetMapping("/api/dashboard")
    public DashboardResponse dashboard() {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        return factory.create(me.getRole()).build(me);
    }
}
