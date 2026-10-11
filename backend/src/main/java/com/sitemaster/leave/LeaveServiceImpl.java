package com.sitemaster.leave;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.employee.CurrentEmployeeProvider;
import com.sitemaster.employee.Employee;
import com.sitemaster.employee.EmployeeRepository;
import com.sitemaster.leave.dto.LeaveApplication;
import com.sitemaster.leave.dto.LeaveBalanceResponse;
import com.sitemaster.leave.dto.LeaveRequestResponse;
import com.sitemaster.leave.dto.LeaveTypeRequest;
import com.sitemaster.leave.dto.LeaveTypeResponse;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.settings.CompanySettings;
import com.sitemaster.settings.SystemSettingsManager;
import com.sitemaster.settings.WorkCalendar;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link LeaveService}. Rules:
 * <ul>
 *   <li>to-date on or after from-date, within one calendar year;</li>
 *   <li>no past dates, except the type's back-dating allowance (sick leave: 3 days);</li>
 *   <li>days are working days (no Sundays or public holidays); at least one;</li>
 *   <li>no overlap with the employee's pending or approved leave;</li>
 *   <li>days cannot exceed the balance left after pending requests;</li>
 *   <li>HR approves (never their own), which deducts the balance; rejection needs remarks.</li>
 * </ul>
 * OOP concept: Abstraction - implements LeaveService.
 */
@Service
public class LeaveServiceImpl implements LeaveService {

    private static final Map<String, String> SORTS = Map.of("fromDate", "fromDate", "createdAt", "createdAt",
            "status", "status");

    private final LeaveTypeRepository typeRepository;
    private final LeaveBalanceRepository balanceRepository;
    private final LeaveRequestRepository requestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final WorkCalendar calendar;
    private final CurrentUserProvider currentUser;
    private final CurrentEmployeeProvider currentEmployee;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public LeaveServiceImpl(LeaveTypeRepository typeRepository, LeaveBalanceRepository balanceRepository,
                            LeaveRequestRepository requestRepository, EmployeeRepository employeeRepository,
                            UserRepository userRepository, WorkCalendar calendar, CurrentUserProvider currentUser,
                            CurrentEmployeeProvider currentEmployee, WorkflowEventPublisher eventPublisher, Clock clock) {
        this.typeRepository = typeRepository;
        this.balanceRepository = balanceRepository;
        this.requestRepository = requestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.calendar = calendar;
        this.currentUser = currentUser;
        this.currentEmployee = currentEmployee;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // ----- types -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<LeaveTypeResponse> types(boolean activeOnly) {
        List<LeaveType> types = activeOnly ? typeRepository.findByActiveTrueOrderByNameAsc()
                : typeRepository.findAllByOrderByNameAsc();
        CompanySettings settings = settings();
        return types.stream().map(t -> toResponse(t, settings)).toList();
    }

    @Override
    @Transactional
    public LeaveTypeResponse createType(LeaveTypeRequest request) {
        if (typeRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw DuplicateRecordException.forField("code", "leaveType.code.duplicate");
        }
        LeaveType type = new LeaveType(request.getCode());
        applyType(type, request);
        typeRepository.save(type);
        return toResponse(type, settings());
    }

    @Override
    @Transactional
    public LeaveTypeResponse updateType(Long id, LeaveTypeRequest request) {
        LeaveType type = typeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Leave type"));
        if (!type.getCode().equalsIgnoreCase(request.getCode())) {
            throw BusinessRuleException.forField("code", "leaveType.code.fixed");
        }
        applyType(type, request);
        return toResponse(type, settings());
    }

    private void applyType(LeaveType type, LeaveTypeRequest r) {
        if (!type.isFromSettings() && r.getDaysPerYear() == null) {
            throw BusinessRuleException.forField("daysPerYear", "field.required");
        }
        type.update(r.getName(), r.getDaysPerYear(), r.isPaid(), r.getBackdateDays(), r.isActive());
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.LEAVE_TYPE_SAVED)
                .entityId(type.getId()).args(type.getName()).build());
    }

    // ----- balances ----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> myBalances(int year) {
        return balancesFor(currentEmployee.requireEmployee(), year);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaveBalanceResponse> balances(Long employeeId, int year) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Employee"));
        return balancesFor(employee, year);
    }

    private List<LeaveBalanceResponse> balancesFor(Employee employee, int year) {
        CompanySettings settings = settings();
        return typeRepository.findByActiveTrueOrderByNameAsc().stream().map(type -> {
            BigDecimal entitled = balanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), type.getId(), year)
                    .map(LeaveBalance::getEntitledDays).orElse(type.entitlement(settings));
            BigDecimal used = balanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), type.getId(), year)
                    .map(LeaveBalance::getUsedDays).orElse(BigDecimal.ZERO);
            BigDecimal pending = requestRepository.pendingDays(employee.getId(), type.getId(), year);
            return new LeaveBalanceResponse(type.getId(), type.getName(), type.isPaid(), year, entitled, used, pending,
                    entitled.subtract(used).subtract(pending).max(BigDecimal.ZERO));
        }).toList();
    }

    /** Balance row for the year, created from the current entitlement on first use. */
    private LeaveBalance balance(Employee employee, LeaveType type, int year) {
        return balanceRepository.findByEmployeeIdAndLeaveTypeIdAndYear(employee.getId(), type.getId(), year)
                .orElseGet(() -> balanceRepository.save(new LeaveBalance(employee, type, year, type.entitlement(settings()))));
    }

    // ----- requests ----------------------------------------------------------------------

    @Override
    @Transactional
    public LeaveRequestResponse apply(LeaveApplication a) {
        Employee me = currentEmployee.requireEmployee();
        LeaveType type = typeRepository.findById(a.getLeaveTypeId()).filter(LeaveType::isActive)
                .orElseThrow(() -> BusinessRuleException.forField("leaveTypeId", "leave.type.unknown"));
        LocalDate today = LocalDate.now(clock);
        if (a.getFromDate().isBefore(today.minusDays(type.getBackdateDays()))) {
            throw BusinessRuleException.forField("fromDate", type.getBackdateDays() == 0 ? "leave.from.past"
                    : "leave.from.backdate", type.getBackdateDays());
        }
        if (a.getFromDate().getYear() != a.getToDate().getYear()) {
            throw BusinessRuleException.forField("toDate", "leave.sameYear");
        }
        int days = calendar.workingDaysBetween(a.getFromDate(), a.getToDate());
        if (days == 0) {
            throw BusinessRuleException.forField("toDate", "leave.noWorkingDays");
        }
        if (requestRepository.existsOverlap(me.getId(), a.getFromDate(), a.getToDate(),
                EnumSet.of(RequestStatus.PENDING, RequestStatus.APPROVED))) {
            throw BusinessRuleException.forField("fromDate", "leave.overlap");
        }
        int year = a.getFromDate().getYear();
        LeaveBalance balance = balance(me, type, year);
        BigDecimal available = balance.remaining().subtract(requestRepository.pendingDays(me.getId(), type.getId(), year));
        BigDecimal requested = BigDecimal.valueOf(days);
        if (requested.compareTo(available) > 0) {
            throw BusinessRuleException.forField("toDate", "leave.balance.insufficient", available.max(BigDecimal.ZERO));
        }
        LeaveRequest request = requestRepository.save(new LeaveRequest(me, type, a.getFromDate(), a.getToDate(), requested,
                a.getReason(), currentUser.requireUser()));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.LEAVE_REQUESTED)
                .entityId(request.getId()).args(type.getName(), days, a.getFromDate(), a.getToDate())
                .recipients(userRepository.findActiveIdsByRole(RoleName.HR_MANAGER).stream()
                        .filter(id -> !id.equals(currentUser.requireId())).toList())
                .link("leave").build());
        return toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestResponse> mine(PageQuery pageQuery) {
        Long employeeId = currentEmployee.requireEmployee().getId();
        Specification<LeaveRequest> spec = (root, q, cb) -> cb.equal(root.get("employee").get("id"), employeeId);
        return PageResponse.from(requestRepository.findAll(spec, pageQuery.toPageable(SORTS, "fromDate", Sort.Direction.DESC)),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestResponse> search(RequestStatus status, Long employeeId, LocalDate from, LocalDate to,
                                                     PageQuery pageQuery) {
        String search = pageQuery.getSearch();
        Specification<LeaveRequest> spec = (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
        spec = spec.and((root, q, cb) -> employeeId == null ? null : cb.equal(root.get("employee").get("id"), employeeId))
                .and((root, q, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("fromDate"), to))
                .and((root, q, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("toDate"), from))
                .and((root, q, cb) -> {
                    if (search == null) {
                        return null;
                    }
                    Join<Object, Object> employee = root.join("employee");
                    Join<Object, Object> user = employee.join("user");
                    String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                    return cb.or(cb.like(cb.lower(user.get("fullName")), like),
                            cb.like(cb.lower(employee.get("employeeNo")), like));
                });
        return PageResponse.from(requestRepository.findAll(spec, pageQuery.toPageable(SORTS, "createdAt", Sort.Direction.DESC)),
                this::toResponse);
    }

    @Override
    @Transactional
    public LeaveRequestResponse approve(Long id, String remarks) {
        LeaveRequest request = find(id);
        request.prepareBalance(balance(request.getEmployee(), request.getLeaveType(), request.getFromDate().getYear()));
        request.approve(currentUser.requireUser(), remarks);
        request.recordDecision(LocalDateTime.now(clock));
        publishDecision(WorkflowEventType.LEAVE_APPROVED, request);
        return toResponse(request);
    }

    @Override
    @Transactional
    public LeaveRequestResponse reject(Long id, String remarks) {
        LeaveRequest request = find(id);
        request.reject(currentUser.requireUser(), remarks);
        request.recordDecision(LocalDateTime.now(clock));
        publishDecision(WorkflowEventType.LEAVE_REJECTED, request);
        return toResponse(request);
    }

    @Override
    @Transactional
    public void cancel(Long id) {
        LeaveRequest request = find(id);
        if (!request.getEmployee().getId().equals(currentEmployee.requireEmployee().getId())) {
            throw new UnauthorizedActionException("leave.cancel.denied");
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidStatusTransitionException("leave.cancel.notPending");
        }
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.LEAVE_CANCELLED)
                .entityId(id).args(request.getLeaveType().getName(), request.getFromDate()).build());
        requestRepository.delete(request);
    }

    private void publishDecision(WorkflowEventType type, LeaveRequest request) {
        eventPublisher.publish(WorkflowEvent.byCurrentUser(type).entityId(request.getId())
                .args(request.getLeaveType().getName(), request.getFromDate(), request.getToDate(),
                        request.getRemarks() == null ? "" : request.getRemarks())
                .recipients(request.getEmployee().getUser().getId()).link("my-leave").build());
    }

    private LeaveRequest find(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Leave request"));
    }

    private static CompanySettings settings() {
        return SystemSettingsManager.getInstance().getSettings();
    }

    private static LeaveTypeResponse toResponse(LeaveType t, CompanySettings settings) {
        return new LeaveTypeResponse(t.getId(), t.getCode(), t.getName(), t.getDaysPerYear(), t.entitlement(settings),
                t.isFromSettings(), t.isPaid(), t.getBackdateDays(), t.isActive());
    }

    private LeaveRequestResponse toResponse(LeaveRequest r) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        boolean own = r.getEmployee().getUser().getId().equals(me.getUserId());
        boolean pending = r.getStatus() == RequestStatus.PENDING;
        boolean canDecide = pending && !own && me.getRole() == RoleName.HR_MANAGER;
        Employee e = r.getEmployee();
        return new LeaveRequestResponse(r.getId(), e.getId(), e.getEmployeeNo(), e.getUser().getFullName(),
                r.getLeaveType().getId(), r.getLeaveType().getName(), r.getFromDate(), r.getToDate(), r.getDays(),
                r.getReason(), r.getStatus().name(), r.getApprovedBy() == null ? null : r.getApprovedBy().getFullName(),
                r.getRemarks(), r.getDecidedAt(), canDecide, pending && own, r.getCreatedAt());
    }
}
