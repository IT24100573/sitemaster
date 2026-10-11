package com.sitemaster.report.generator;

import com.sitemaster.attendance.Attendance;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.report.ReportCriteria;
import com.sitemaster.report.ReportTable;
import com.sitemaster.report.ReportType;
import com.sitemaster.security.CurrentUserProvider;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.sitemaster.report.generator.ReportSupport.col;
import static com.sitemaster.report.generator.ReportSupport.row;

/**
 * Attendance totals per employee in the period. PMs and site engineers must
 * choose one of their projects; HR and Admin may report on all sites.
 */
@Component
public class AttendanceReport implements ReportGenerator {

    private final ReportSupport support;
    private final CurrentUserProvider currentUser;

    public AttendanceReport(ReportSupport support, CurrentUserProvider currentUser) {
        this.support = support;
        this.currentUser = currentUser;
    }

    @Override
    public ReportType type() {
        return ReportType.ATTENDANCE;
    }

    @Override
    public ReportTable generate(ReportCriteria criteria) {
        boolean companyWide = EnumSet.of(RoleName.ADMIN, RoleName.HR_MANAGER).contains(currentUser.requirePrincipal().getRole());
        if (!companyWide && criteria.projectId() == null) {
            throw BusinessRuleException.forField("projectId", "report.projectRequired");
        }
        // HR and Admin report on every site (HR is not a project member); others only on their own projects.
        List<Long> projects = companyWide
                ? (criteria.projectId() == null ? null : List.of(criteria.projectId()))
                : support.projectScope(criteria);
        TypedQuery<Attendance> query = support.em().createQuery("select a from Attendance a join fetch a.employee e join fetch e.user "
                + "where a.date between :from and :to" + (projects == null ? "" : " and a.project.id in :projects"),
                Attendance.class).setParameter("from", criteria.from()).setParameter("to", criteria.to());
        if (projects != null) {
            query.setParameter("projects", projects);
        }
        Map<Long, int[]> totals = new LinkedHashMap<>();
        Map<Long, Attendance> sample = new LinkedHashMap<>();
        for (Attendance a : query.getResultList()) {
            int[] t = totals.computeIfAbsent(a.getEmployee().getId(), k -> new int[5]);
            sample.putIfAbsent(a.getEmployee().getId(), a);
            switch (a.getStatus()) {
                case PRESENT -> t[0]++;
                case HALF_DAY -> t[1]++;
                case ABSENT -> t[2]++;
                case ON_LEAVE -> t[3]++;
            }
            if (!a.isVerified()) {
                t[4]++;
            }
        }
        List<List<Object>> rows = new ArrayList<>();
        sample.values().stream()
                .sorted(Comparator.comparing(a -> a.getEmployee().getEmployeeNo()))
                .forEach(a -> {
                    int[] t = totals.get(a.getEmployee().getId());
                    rows.add(row(a.getEmployee().getEmployeeNo(), a.getEmployee().getUser().getFullName(),
                            a.getEmployee().getTrade(), t[0], t[1], t[2], t[3], t[4]));
                });
        return new ReportTable(type().getTitle(), support.period(criteria),
                List.of(col("Employee no", ReportTable.Kind.TEXT), col("Name", ReportTable.Kind.TEXT),
                        col("Trade", ReportTable.Kind.TEXT), col("Present", ReportTable.Kind.NUMBER),
                        col("Half days", ReportTable.Kind.NUMBER), col("Absent", ReportTable.Kind.NUMBER),
                        col("On leave", ReportTable.Kind.NUMBER), col("Not verified", ReportTable.Kind.NUMBER)),
                rows, null);
    }
}
