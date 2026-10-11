package com.sitemaster.dashboard.role;

// PATTERN: Factory Method

import com.sitemaster.common.enums.AttendanceStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.dashboard.DashboardQueries;
import com.sitemaster.dashboard.RoleDashboard;
import com.sitemaster.dashboard.dto.DashboardResponse;
import com.sitemaster.payroll.Payslip;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.task.Task;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static com.sitemaster.dashboard.DashboardQueries.stat;

/** Factory Method pattern - CONCRETE PRODUCT for a Worker: my day, my tasks, my pay. */
@Component
public class WorkerDashboard implements RoleDashboard {

    private final DashboardQueries q;
    private final Clock clock;

    public WorkerDashboard(DashboardQueries q, Clock clock) {
        this.q = q;
        this.clock = clock;
    }

    @Override
    public RoleName role() {
        return RoleName.WORKER;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse build(SiteMasterUserPrincipal user) {
        Long me = user.getUserId();
        LocalDate today = LocalDate.now(clock);
        YearMonth month = YearMonth.from(today);
        BigDecimal checkedIn = q.count("select count(a) from Attendance a where a.employee.user.id = :me and a.date = :d",
                "me", me, "d", today);
        BigDecimal daysWorked = q.count("select count(a) from Attendance a where a.employee.user.id = :me "
                + "and a.date between :from and :to and a.status in :s", "me", me, "from", month.atDay(1), "to", today,
                "s", List.of(AttendanceStatus.PRESENT, AttendanceStatus.HALF_DAY));
        BigDecimal tasks = q.count("select count(t) from Task t where t.assignee.id = :me and t.status <> :done",
                "me", me, "done", TaskStatus.DONE);
        BigDecimal leave = q.sum("select sum(b.entitledDays - b.usedDays) from LeaveBalance b "
                + "where b.employee.user.id = :me and b.year = :y and b.leaveType.code = 'ANNUAL'", "me", me,
                "y", today.getYear());
        List<Payslip> slips = q.list("select p from Payslip p where p.employee.user.id = :me and p.paid = true "
                + "order by p.paidAt desc", Payslip.class, 1, "me", me);
        BigDecimal lastPay = slips.isEmpty() ? BigDecimal.ZERO : slips.get(0).getNetPay();

        List<DashboardResponse.Item> myTasks = q.list("select t from Task t where t.assignee.id = :me "
                        + "and t.status <> :done order by t.dueDate", Task.class, 8, "me", me, "done", TaskStatus.DONE)
                .stream().map(t -> new DashboardResponse.Item(t.getTitle(), t.getProject().getName() + ", due "
                        + t.getDueDate(), t.getStatus().name(), "my-tasks")).toList();

        return new DashboardResponse("My day", checkedIn.signum() > 0 ? "You are checked in today."
                : "You have not checked in today.",
                List.of(stat("Checked in today", checkedIn, "number", checkedIn.signum() > 0 ? "good" : "warning",
                                "my-attendance", checkedIn.signum() > 0 ? "Yes" : "Not yet"),
                        stat("Days worked this month", daysWorked, "number", "neutral", "my-attendance", null),
                        stat("Annual leave left", leave, "number", "neutral", "my-leave", "Days"),
                        stat("Last salary", lastPay, "money", "neutral", "payslips", null)),
                List.of(),
                List.of(new DashboardResponse.ItemList("My tasks", "No open tasks.", "my-tasks", myTasks)));
    }
}
