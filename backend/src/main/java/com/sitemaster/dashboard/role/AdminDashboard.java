package com.sitemaster.dashboard.role;

// PATTERN: Factory Method

import com.sitemaster.common.enums.ApplicationStatus;
import com.sitemaster.common.enums.ProjectStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.dashboard.DashboardQueries;
import com.sitemaster.dashboard.RoleDashboard;
import com.sitemaster.dashboard.dto.DashboardResponse;
import com.sitemaster.finance.ProjectFinanceService;
import com.sitemaster.finance.dto.ProjectFinanceResponse;
import com.sitemaster.project.Project;
import com.sitemaster.security.SiteMasterUserPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static com.sitemaster.dashboard.DashboardQueries.attention;
import static com.sitemaster.dashboard.DashboardQueries.stat;

/** Factory Method pattern - CONCRETE PRODUCT for the Admin: company-wide picture. */
@Component
public class AdminDashboard implements RoleDashboard {

    private static final Set<String> RUNNING = Set.of("ACTIVE", "DELAYED");
    private static final int MAX_BARS = 8;

    private final DashboardQueries q;
    private final ProjectFinanceService finance;

    public AdminDashboard(DashboardQueries q, ProjectFinanceService finance) {
        this.q = q;
        this.finance = finance;
    }

    @Override
    public RoleName role() {
        return RoleName.ADMIN;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse build(SiteMasterUserPrincipal user) {
        BigDecimal active = q.count("select count(p) from Project p where p.status = :s", "s", ProjectStatus.ACTIVE);
        BigDecimal delayed = q.count("select count(p) from Project p where p.status = :s", "s", ProjectStatus.DELAYED);
        BigDecimal users = q.count("select count(u) from User u where u.active = true");
        BigDecimal applications = q.count("select count(a) from SupplierApplication a where a.status = :s",
                "s", ApplicationStatus.PENDING);

        List<DashboardResponse.Point> byStatus = q.rows("select p.status, count(p) from Project p group by p.status")
                .stream().map(r -> new DashboardResponse.Point(((ProjectStatus) r[0]).name(),
                        List.of(BigDecimal.valueOf((Long) r[1])))).toList();
        List<DashboardResponse.Point> budgets = finance.overview().stream()
                .filter(f -> RUNNING.contains(f.status())).limit(MAX_BARS)
                .map(f -> new DashboardResponse.Point(f.projectName(), List.of(f.budget(), used(f))))
                .toList();
        List<DashboardResponse.Item> delayedProjects = q.list("select p from Project p where p.status = :s "
                + "order by p.endDate", Project.class, 6, "s", ProjectStatus.DELAYED).stream()
                .map(p -> new DashboardResponse.Item(p.getName(), p.getProgress() + "% complete, due " + p.getEndDate(),
                        p.getStatus().name(), "projects/" + p.getId())).toList();

        return new DashboardResponse("Company overview", "Projects, people and suppliers across SiteMaster",
                List.of(stat("Active projects", active, "number", "neutral", "projects", null),
                        stat("Delayed projects", delayed, "number", delayed.signum() > 0 ? "danger" : "good", "projects", null),
                        stat("Active users", users, "number", "neutral", "users", null),
                        stat("Supplier applications", applications, "number", attention(applications),
                                "supplier-applications", "Waiting for review")),
                List.of(new DashboardResponse.Chart("Projects by status", "pie", "number", List.of("Projects"), byStatus),
                        new DashboardResponse.Chart("Budget vs spent and committed", "bar", "money",
                                List.of("Budget", "Spent + committed"), budgets)),
                List.of(new DashboardResponse.ItemList("Delayed projects", "No project is delayed.", "projects",
                        delayedProjects)));
    }

    private static BigDecimal used(ProjectFinanceResponse f) {
        return f.totalSpent().add(f.committed());
    }
}
