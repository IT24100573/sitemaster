package com.sitemaster.support;

import com.sitemaster.client.Client;
import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.employee.Employee;
import com.sitemaster.material.Material;
import com.sitemaster.project.Phase;
import com.sitemaster.project.Project;
import com.sitemaster.role.Role;
import com.sitemaster.settings.SystemSettingsManager;
import com.sitemaster.user.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Builders for entities used across unit tests. Ids are set by reflection,
 * exactly as JPA would after saving.
 */
public final class TestData {

    public static final LocalDate TODAY = LocalDate.of(2026, 10, 7);

    private TestData() {
        // utility class
    }

    public static <T> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    public static User user(long id, RoleName role) {
        Role r = new Role(role, role.getDisplayName(), null);
        return withId(new User("User " + id, "user" + id + "@sitemaster.lk", "hash", r), id);
    }

    public static Project project(long id, LocalDate start, LocalDate end, String budget) {
        Client client = withId(new Client("Client"), 1);
        Project project = new Project("Project " + id, client, user(100, RoleName.PROJECT_MANAGER));
        project.schedule(start, end);
        project.updateDetails("Project " + id, null, "Colombo", null, null, new BigDecimal(budget));
        return withId(project, id);
    }

    public static Phase phase(long id, Project project, LocalDate start, LocalDate end, String budget) {
        Phase phase = new Phase(project);
        phase.update("Phase " + id, null, start, end, new BigDecimal(budget), 1);
        return withId(phase, id);
    }

    public static Material material(long id, String unitPrice) {
        Material material = new Material("Material " + id);
        material.update("Material " + id, "Cement", "bag", new BigDecimal(unitPrice), null);
        return withId(material, id);
    }

    public static Employee dailyWage(long id, String rate) {
        Employee employee = new Employee(user(id, RoleName.WORKER), "EMP-" + id);
        employee.applyPayTerms(EmploymentType.DAILY_WAGE, new BigDecimal(rate), null, null, null, null);
        return withId(employee, id);
    }

    public static Employee permanent(long id, String salary) {
        Employee employee = new Employee(user(id, RoleName.SITE_ENGINEER), "EMP-" + id);
        employee.applyPayTerms(EmploymentType.PERMANENT, null, new BigDecimal(salary), null, null, null);
        return withId(employee, id);
    }

    public static Employee contract(long id, String amount, LocalDate start, LocalDate end) {
        Employee employee = new Employee(user(id, RoleName.WORKER), "EMP-" + id);
        employee.applyPayTerms(EmploymentType.CONTRACT, null, null, new BigDecimal(amount), start, end);
        return withId(employee, id);
    }

    /** Loads System Settings used by payroll and calendar logic (08:00-17:00 day, 1.5x overtime, 8% EPF). */
    public static void loadSettings(String... holidays) {
        Map<String, String> values = new HashMap<>();
        values.put("company.name", "SiteMaster Test");
        values.put("finance.currency", "LKR");
        values.put("finance.receiptThreshold", "5000.00");
        values.put("work.startTime", "08:00");
        values.put("work.endTime", "17:00");
        values.put("work.holidays", String.join(",", holidays));
        values.put("leave.annualDays", "14");
        values.put("leave.casualDays", "7");
        values.put("leave.sickDays", "7");
        values.put("payroll.epfEmployeeRate", "8.00");
        values.put("payroll.epfEmployerRate", "12.00");
        values.put("payroll.etfRate", "3.00");
        values.put("payroll.overtimeMultiplier", "1.50");
        values.put("payroll.siteAllowancePerDay", "250.00");
        values.put("employee.trades", "Mason,Carpenter");
        values.put("material.categories", "Cement,Steel");
        values.put("material.units", "bag,kg");
        SystemSettingsManager.getInstance().reload(values);
    }
}
