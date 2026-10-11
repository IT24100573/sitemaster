package com.sitemaster.lookup;

import com.sitemaster.client.ClientRepository;
import com.sitemaster.common.dto.LookupOption;
import com.sitemaster.common.enums.ApplicationStatus;
import com.sitemaster.common.enums.AttendanceStatus;
import com.sitemaster.common.enums.DocumentCategory;
import com.sitemaster.common.enums.PayrollStatus;
import com.sitemaster.common.enums.EquipmentStatus;
import com.sitemaster.common.enums.ExpenseCategory;
import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.common.enums.PurchaseOrderStatus;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.WeatherCondition;
import com.sitemaster.common.enums.IssueSeverity;
import com.sitemaster.common.enums.IssueStatus;
import com.sitemaster.common.enums.IssueType;
import com.sitemaster.common.enums.PhaseStatus;
import com.sitemaster.common.enums.ProjectStatus;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.common.enums.Urgency;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.enums.AppModule;
import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.util.EnumLabels;
import com.sitemaster.employee.Employee;
import com.sitemaster.employee.EmployeeDocumentType;
import com.sitemaster.role.RoleRepository;
import com.sitemaster.settings.SystemSettingsManager;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import com.sitemaster.user.UserSpecifications;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link LookupService}.
 * <p>
 * OOP concept: Abstraction - implements LookupService.
 */
@Service
@Transactional(readOnly = true)
public class LookupServiceImpl implements LookupService {

    private static final int MAX_USER_RESULTS = 20;
    private static final int MAX_UNLINKED_RESULTS = 200;

    /** Enumerations the frontend may request by name. */
    private static final Map<String, Enum<?>[]> ENUMS = Map.ofEntries(
            Map.entry("project-statuses", ProjectStatus.values()),
            Map.entry("phase-statuses", PhaseStatus.values()),
            Map.entry("task-statuses", TaskStatus.values()),
            Map.entry("urgencies", Urgency.values()),
            Map.entry("issue-types", IssueType.values()),
            Map.entry("issue-severities", IssueSeverity.values()),
            Map.entry("issue-statuses", IssueStatus.values()),
            Map.entry("document-categories", DocumentCategory.values()),
            Map.entry("application-statuses", ApplicationStatus.values()),
            Map.entry("request-statuses", RequestStatus.values()),
            Map.entry("equipment-statuses", EquipmentStatus.values()),
            Map.entry("weather-conditions", WeatherCondition.values()),
            Map.entry("purchase-order-statuses", PurchaseOrderStatus.values()),
            Map.entry("payment-methods", PaymentMethod.values()),
            Map.entry("invoice-statuses", InvoiceStatus.values()),
            Map.entry("attendance-statuses", AttendanceStatus.values()),
            Map.entry("payroll-statuses", PayrollStatus.values()),
            Map.entry("expense-categories", ExpenseCategory.values()));

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;

    public LookupServiceImpl(RoleRepository roleRepository, UserRepository userRepository,
                             ClientRepository clientRepository) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.clientRepository = clientRepository;
    }

    @Override
    public List<LookupOption> roles() {
        return roleRepository.findAll(Sort.by("id")).stream()
                .map(role -> new LookupOption(role.getName().name(), role.getDisplayName()))
                .toList();
    }

    @Override
    public List<LookupOption> employmentTypes() {
        return enumOptions(EmploymentType.values());
    }

    @Override
    public List<LookupOption> trades() {
        return SystemSettingsManager.getInstance().getSettings().trades().stream()
                .map(trade -> new LookupOption(trade, trade))
                .toList();
    }

    @Override
    public List<LookupOption> modules() {
        return enumOptions(AppModule.values());
    }

    @Override
    public List<LookupOption> employeeDocumentTypes() {
        return enumOptions(EmployeeDocumentType.values());
    }

    @Override
    public List<LookupOption> enumValues(String name) {
        Enum<?>[] values = ENUMS.get(name);
        if (values == null) {
            throw new ResourceNotFoundException("resource.notFound", "Lookup");
        }
        return enumOptions(values);
    }

    @Override
    public List<LookupOption> materialCategories() {
        return SystemSettingsManager.getInstance().getSettings().materialCategories().stream()
                .map(category -> new LookupOption(category, category))
                .toList();
    }

    @Override
    public List<LookupOption> materialUnits() {
        return SystemSettingsManager.getInstance().getSettings().materialUnits().stream()
                .map(unit -> new LookupOption(unit, unit))
                .toList();
    }

    @Override
    public List<LookupOption> activeClients() {
        return clientRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> new LookupOption(String.valueOf(c.getId()), c.getName()))
                .toList();
    }

    @Override
    public List<LookupOption> projectManagers() {
        return userRepository.findAll(Specification.where(UserSpecifications.active(true))
                                .and(UserSpecifications.role(RoleName.PROJECT_MANAGER)), Sort.by("fullName"))
                .stream()
                .map(u -> new LookupOption(String.valueOf(u.getId()), u.getFullName()))
                .toList();
    }

    @Override
    public List<LookupOption> unlinkedStaff() {
        Specification<User> noEmployee = (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<Employee> employee = sub.from(Employee.class);
            sub.select(employee.get("user").get("id"));
            return cb.not(root.get("id").in(sub));
        };
        Specification<User> spec = Specification.where(UserSpecifications.active(true))
                .and((root, query, cb) -> cb.notEqual(root.get("role").get("name"), RoleName.SUPPLIER))
                .and(noEmployee);
        return userRepository.findAll(spec, PageRequest.of(0, MAX_UNLINKED_RESULTS, Sort.by("fullName")))
                .stream()
                .map(u -> new LookupOption(String.valueOf(u.getId()),
                        u.getFullName() + " (" + u.getRole().getDisplayName() + ", " + u.getEmail() + ")"))
                .toList();
    }

    @Override
    public List<LookupOption> users(String search) {
        return userRepository.findAll(UserSpecifications.text(search),
                        PageRequest.of(0, MAX_USER_RESULTS, Sort.by("fullName")))
                .stream()
                .map(u -> new LookupOption(String.valueOf(u.getId()), u.getFullName() + " (" + u.getEmail() + ")"))
                .toList();
    }

    private static List<LookupOption> enumOptions(Enum<?>[] values) {
        return Arrays.stream(values).map(v -> new LookupOption(v.name(), EnumLabels.humanize(v))).toList();
    }
}
