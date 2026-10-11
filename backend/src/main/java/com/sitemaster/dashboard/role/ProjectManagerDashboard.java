package com.sitemaster.dashboard.role;

// PATTERN: Factory Method

import com.sitemaster.common.enums.IssueStatus;
import com.sitemaster.common.enums.ProjectStatus;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.dailyreport.DailyReport;
import com.sitemaster.dashboard.DashboardQueries;
import com.sitemaster.dashboard.RoleDashboard;
import com.sitemaster.dashboard.dto.DashboardResponse;
import com.sitemaster.expense.ExpenseRequest;
import com.sitemaster.materialrequest.MaterialRequest;
import com.sitemaster.project.Project;
import com.sitemaster.security.SiteMasterUserPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static com.sitemaster.dashboard.DashboardQueries.attention;
import static com.sitemaster.dashboard.DashboardQueries.stat;

/** Factory Method pattern - CONCRETE PRODUCT for a Project Manager: their projects and what waits for them. */
@Component
public class ProjectManagerDashboard implements RoleDashboard {

    private static final int MAX_ITEMS = 8;

    private final DashboardQueries q;
    private final Clock clock;

    public ProjectManagerDashboard(DashboardQueries q, Clock clock) {
        this.q = q;
        this.clock = clock;
    }

    @Override
    public RoleName role() {
        return RoleName.PROJECT_MANAGER;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse build(SiteMasterUserPrincipal user) {
        Long me = user.getUserId();
        LocalDate today = LocalDate.now(clock);
        BigDecimal projects = q.count("select count(p) from Project p where p.manager.id = :me and p.status in :s",
                "me", me, "s", EnumSet.of(ProjectStatus.ACTIVE, ProjectStatus.DELAYED, ProjectStatus.PLANNED));
        BigDecimal requests = q.count("select count(r) from MaterialRequest r where r.project.manager.id = :me "
                + "and r.status = :s", "me", me, "s", RequestStatus.PENDING);
        BigDecimal reports = q.count("select count(r) from DailyReport r where r.project.manager.id = :me "
                + "and r.status = :s", "me", me, "s", RequestStatus.PENDING);
        BigDecimal expenses = q.count("select count(e) from ExpenseRequest e where e.project.manager.id = :me "
                + "and e.status = :s", "me", me, "s", RequestStatus.PENDING);
        BigDecimal approvals = requests.add(reports).add(expenses);
        BigDecimal issues = q.count("select count(i) from Issue i where i.project.manager.id = :me and i.status in :s",
                "me", me, "s", EnumSet.of(IssueStatus.OPEN, IssueStatus.IN_PROGRESS));
        BigDecimal overdue = q.count("select count(t) from Task t where t.project.manager.id = :me "
                + "and t.status <> :done and t.dueDate < :today", "me", me, "done", TaskStatus.DONE, "today", today);

        List<DashboardResponse.Point> progress = q.list("select p from Project p where p.manager.id = :me "
                        + "and p.status in :s order by p.name", Project.class, MAX_ITEMS, "me", me,
                        "s", EnumSet.of(ProjectStatus.ACTIVE, ProjectStatus.DELAYED)).stream()
                .map(p -> new DashboardResponse.Point(p.getName(), List.of(BigDecimal.valueOf(p.getProgress()))))
                .toList();

        List<DashboardResponse.Item> waiting = new ArrayList<>();
        q.list("select r from MaterialRequest r where r.project.manager.id = :me and r.status = :s order by r.createdAt",
                MaterialRequest.class, MAX_ITEMS, "me", me, "s", RequestStatus.PENDING)
                .forEach(r -> waiting.add(new DashboardResponse.Item("Material: " + r.getQuantity().stripTrailingZeros()
                        .toPlainString() + " " + r.getMaterial().getUnit() + " " + r.getMaterial().getName(),
                        r.getProject().getName() + ", " + r.getRequestedBy().getFullName(), r.getUrgency().name(),
                        "material-requests/" + r.getId())));
        q.list("select e from ExpenseRequest e where e.project.manager.id = :me and e.status = :s order by e.createdAt",
                ExpenseRequest.class, MAX_ITEMS, "me", me, "s", RequestStatus.PENDING)
                .forEach(e -> waiting.add(new DashboardResponse.Item("Expense: " + e.getPayee(),
                        e.getProject().getName() + ", " + e.getAmount().toPlainString(), "PENDING", "expenses/" + e.getId())));
        q.list("select r from DailyReport r where r.project.manager.id = :me and r.status = :s order by r.reportDate",
                DailyReport.class, MAX_ITEMS, "me", me, "s", RequestStatus.PENDING)
                .forEach(r -> waiting.add(new DashboardResponse.Item("Daily report " + r.getReportDate(),
                        r.getProject().getName() + ", " + r.getSubmittedBy().getFullName(), "PENDING",
                        "daily-reports/" + r.getId())));

        return new DashboardResponse("My projects", "What needs your decision today",
                List.of(stat("Active projects", projects, "number", "neutral", "projects", null),
                        stat("Waiting for approval", approvals, "number", attention(approvals), "approvals",
                                requests + " material, " + reports + " reports, " + expenses + " expenses"),
                        stat("Open issues", issues, "number", attention(issues), "projects", null),
                        stat("Overdue tasks", overdue, "number", overdue.signum() > 0 ? "danger" : "good", "my-tasks", null)),
                List.of(new DashboardResponse.Chart("Progress by project", "bar", "percent", List.of("Progress"), progress)),
                List.of(new DashboardResponse.ItemList("Waiting for you", "Nothing to approve.", "approvals",
                        waiting.stream().limit(MAX_ITEMS).toList())));
    }
}
