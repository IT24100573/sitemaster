package com.sitemaster.dashboard.role;

// PATTERN: Factory Method

import com.sitemaster.common.enums.AttendanceStatus;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.dashboard.DashboardQueries;
import com.sitemaster.dashboard.RoleDashboard;
import com.sitemaster.dashboard.dto.DashboardResponse;
import com.sitemaster.materialrequest.MaterialRequest;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.task.Task;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.sitemaster.dashboard.DashboardQueries.attention;
import static com.sitemaster.dashboard.DashboardQueries.stat;

/** Factory Method pattern - CONCRETE PRODUCT for a Site Engineer: today on site. */
@Component
public class SiteEngineerDashboard implements RoleDashboard {

    private static final String MY_PROJECTS = "(select m.project.id from ProjectMember m where m.user.id = :me)";
    private static final int DAYS = 7;
    private static final int MAX_ITEMS = 8;

    private final DashboardQueries q;
    private final Clock clock;

    public SiteEngineerDashboard(DashboardQueries q, Clock clock) {
        this.q = q;
        this.clock = clock;
    }

    @Override
    public RoleName role() {
        return RoleName.SITE_ENGINEER;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse build(SiteMasterUserPrincipal user) {
        Long me = user.getUserId();
        LocalDate today = LocalDate.now(clock);
        BigDecimal toVerify = q.count("select count(a) from Attendance a where a.verified = false "
                + "and a.employee.user.id <> :me and a.project.id in " + MY_PROJECTS, "me", me);
        BigDecimal myPending = q.count("select count(r) from MaterialRequest r where r.requestedBy.id = :me "
                + "and r.status = :s", "me", me, "s", RequestStatus.PENDING)
                .add(q.count("select count(e) from ExpenseRequest e where e.requestedBy.id = :me and e.status = :s",
                        "me", me, "s", RequestStatus.PENDING));
        BigDecimal tasks = q.count("select count(t) from Task t where t.assignee.id = :me and t.status <> :done",
                "me", me, "done", TaskStatus.DONE);
        BigDecimal deliveries = q.count("select count(r) from MaterialRequest r where r.status = :s "
                + "and r.project.id in " + MY_PROJECTS, "me", me, "s", RequestStatus.PAID);

        List<DashboardResponse.Point> attendance = new ArrayList<>();
        for (int i = DAYS - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            BigDecimal present = q.count("select count(a) from Attendance a where a.date = :d and a.status in :s "
                    + "and a.project.id in " + MY_PROJECTS, "d", day, "me", me,
                    "s", List.of(AttendanceStatus.PRESENT, AttendanceStatus.HALF_DAY));
            attendance.add(new DashboardResponse.Point(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    + " " + day.getDayOfMonth(), List.of(present)));
        }

        List<DashboardResponse.Item> todo = new ArrayList<>();
        q.list("select r from MaterialRequest r where r.status = :s and r.project.id in " + MY_PROJECTS
                        + " order by r.requiredBy", MaterialRequest.class, MAX_ITEMS, "me", me, "s", RequestStatus.PAID)
                .forEach(r -> todo.add(new DashboardResponse.Item("Confirm delivery: " + r.getMaterial().getName(),
                        r.getProject().getName() + ", needed " + r.getRequiredBy(), "PAID", "material-requests/" + r.getId())));
        q.list("select t from Task t where t.assignee.id = :me and t.status <> :done order by t.dueDate", Task.class,
                        MAX_ITEMS, "me", me, "done", TaskStatus.DONE)
                .forEach(t -> todo.add(new DashboardResponse.Item(t.getTitle(), t.getProject().getName() + ", due "
                        + t.getDueDate(), t.getStatus().name(), "my-tasks")));

        return new DashboardResponse("Site today", "Your sites, your crew, your requests",
                List.of(stat("Attendance to verify", toVerify, "number", attention(toVerify), "attendance", null),
                        stat("Deliveries to confirm", deliveries, "number", attention(deliveries), "material-requests", null),
                        stat("My pending requests", myPending, "number", "neutral", "material-requests", "Materials and expenses"),
                        stat("My open tasks", tasks, "number", "neutral", "my-tasks", null)),
                List.of(new DashboardResponse.Chart("Workers on site, last 7 days", "bar", "number", List.of("Present"),
                        attendance)),
                List.of(new DashboardResponse.ItemList("To do", "Nothing waiting.", null,
                        todo.stream().limit(MAX_ITEMS).toList())));
    }
}
