package com.sitemaster.dashboard.role;

// PATTERN: Factory Method

import com.sitemaster.common.enums.AttendanceStatus;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.dashboard.DashboardQueries;
import com.sitemaster.dashboard.RoleDashboard;
import com.sitemaster.dashboard.dto.DashboardResponse;
import com.sitemaster.hr.HrRecordsServiceImpl;
import com.sitemaster.leave.LeaveRequest;
import com.sitemaster.security.SiteMasterUserPrincipal;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static com.sitemaster.dashboard.DashboardQueries.attention;
import static com.sitemaster.dashboard.DashboardQueries.stat;

/** Factory Method pattern - CONCRETE PRODUCT for HR: people, time off and contracts. */
@Component
public class HrDashboard implements RoleDashboard {

    private final DashboardQueries q;
    private final Clock clock;

    public HrDashboard(DashboardQueries q, Clock clock) {
        this.q = q;
        this.clock = clock;
    }

    @Override
    public RoleName role() {
        return RoleName.HR_MANAGER;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse build(SiteMasterUserPrincipal user) {
        LocalDate today = LocalDate.now(clock);
        BigDecimal employees = q.count("select count(e) from Employee e where e.user.active = true");
        BigDecimal leave = q.count("select count(r) from LeaveRequest r where r.status = :s", "s", RequestStatus.PENDING);
        BigDecimal overtime = q.count("select count(o) from Overtime o where o.status = :s", "s", RequestStatus.PENDING);
        BigDecimal contracts = q.count("select count(c) from Contract c where c.endDate between :a and :b",
                "a", today, "b", today.plusDays(HrRecordsServiceImpl.REMINDER_DAYS));

        List<DashboardResponse.Point> todayByStatus = q.rows("select a.status, count(a) from Attendance a "
                        + "where a.date = :d group by a.status", "d", today).stream()
                .map(r -> new DashboardResponse.Point(((AttendanceStatus) r[0]).name(),
                        List.of(BigDecimal.valueOf((Long) r[1])))).toList();

        List<DashboardResponse.Item> pendingLeave = q.list("select r from LeaveRequest r where r.status = :s "
                        + "order by r.fromDate", LeaveRequest.class, 8, "s", RequestStatus.PENDING).stream()
                .map(r -> new DashboardResponse.Item(r.getEmployee().getUser().getFullName() + ": " + r.getLeaveType().getName(),
                        r.getFromDate() + " to " + r.getToDate() + ", " + r.getDays().stripTrailingZeros().toPlainString()
                                + " day(s)", "PENDING", "leave")).toList();

        return new DashboardResponse("People", "Attendance, leave and contracts",
                List.of(stat("Active employees", employees, "number", "neutral", "employees", null),
                        stat("Leave to decide", leave, "number", attention(leave), "leave", null),
                        stat("Overtime to approve", overtime, "number", attention(overtime), "overtime", null),
                        stat("Contracts ending in 30 days", contracts, "number", attention(contracts), "contracts", null)),
                List.of(new DashboardResponse.Chart("Attendance today", "pie", "number", List.of("Employees"), todayByStatus)),
                List.of(new DashboardResponse.ItemList("Leave requests", "No leave waiting.", "leave", pendingLeave)));
    }
}
