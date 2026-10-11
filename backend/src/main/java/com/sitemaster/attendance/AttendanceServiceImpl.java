package com.sitemaster.attendance;

import com.sitemaster.attendance.dto.AttendanceRequest;
import com.sitemaster.attendance.dto.AttendanceResponse;
import com.sitemaster.attendance.dto.AttendanceSheetRow;
import com.sitemaster.attendance.dto.OvertimeResponse;
import com.sitemaster.attendance.dto.TodayResponse;
import com.sitemaster.common.dto.LookupOption;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.AttendanceStatus;
import com.sitemaster.common.enums.ProjectStatus;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.employee.CurrentEmployeeProvider;
import com.sitemaster.employee.Employee;
import com.sitemaster.employee.EmployeeRepository;
import com.sitemaster.leave.LeaveRequestRepository;
import com.sitemaster.payroll.AttendanceLock;
import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.project.ProjectMemberRepository;
import com.sitemaster.project.ProjectRepository;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.user.User;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of {@link AttendanceService}. Rules:
 * <ul>
 *   <li>one record per employee per date; date not in the future;</li>
 *   <li>check-out after check-in, at most 16 hours; overtime 0-8 hours on a working day;</li>
 *   <li>the employee must be on the project; no PRESENT/HALF_DAY on approved leave;</li>
 *   <li>self check-in is unverified; a site engineer verifies workers on their projects
 *       (never their own record); HR may correct or verify anything;</li>
 *   <li>once HR sends the month's payroll summary the month is read-only.</li>
 * </ul>
 * OOP concept: Abstraction - implements AttendanceService.
 */
@Service
public class AttendanceServiceImpl implements AttendanceService {

    private static final Set<RoleName> SEE_ALL = EnumSet.of(RoleName.ADMIN, RoleName.HR_MANAGER, RoleName.ACCOUNTANT);
    private static final Set<ProjectStatus> CHECK_IN_STATUSES =
            EnumSet.of(ProjectStatus.PLANNED, ProjectStatus.ACTIVE, ProjectStatus.DELAYED);
    private static final Set<AttendanceStatus> WORKING = EnumSet.of(AttendanceStatus.PRESENT, AttendanceStatus.HALF_DAY);
    private static final Map<String, String> SORTS = Map.of("date", "date", "status", "status", "createdAt", "createdAt");
    private static final Map<String, String> OT_SORTS = Map.of("createdAt", "createdAt", "hours", "hours");

    private final AttendanceRepository attendanceRepository;
    private final OvertimeRepository overtimeRepository;
    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final LeaveRequestRepository leaveRepository;
    private final UserRepository userRepository;
    private final ProjectAccess access;
    private final AttendanceLock lock;
    private final CurrentUserProvider currentUser;
    private final CurrentEmployeeProvider currentEmployee;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public AttendanceServiceImpl(AttendanceRepository attendanceRepository, OvertimeRepository overtimeRepository,
                                 ShiftRepository shiftRepository, EmployeeRepository employeeRepository,
                                 ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
                                 LeaveRequestRepository leaveRepository, UserRepository userRepository,
                                 ProjectAccess access, AttendanceLock lock, CurrentUserProvider currentUser,
                                 CurrentEmployeeProvider currentEmployee, WorkflowEventPublisher eventPublisher,
                                 Clock clock) {
        this.attendanceRepository = attendanceRepository;
        this.overtimeRepository = overtimeRepository;
        this.shiftRepository = shiftRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.lock = lock;
        this.currentUser = currentUser;
        this.currentEmployee = currentEmployee;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // ----- self service --------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public TodayResponse today() {
        Employee me = currentEmployee.requireEmployee();
        LocalDate today = LocalDate.now(clock);
        List<LookupOption> projects = projectRepository.findAll(access.visibleProjects()).stream()
                .filter(p -> CHECK_IN_STATUSES.contains(p.getStatus()) && memberRepository.existsByProjectIdAndUserId(p.getId(), me.getUser().getId()))
                .sorted(Comparator.comparing(Project::getName))
                .map(p -> new LookupOption(String.valueOf(p.getId()), p.getName()))
                .toList();
        return new TodayResponse(attendanceRepository.findByEmployeeIdAndDate(me.getId(), today).map(this::toResponse)
                .orElse(null), projects, onApprovedLeave(me, today));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupOption> team(Long projectId) {
        viewableProject(projectId);
        List<Long> userIds = memberRepository.findWithUsers(projectId).stream()
                .map(m -> m.getUser().getId()).toList();
        return employeeRepository.findByUserIdIn(userIds).stream()
                .sorted(Comparator.comparing(Employee::getEmployeeNo))
                .map(e -> new LookupOption(String.valueOf(e.getId()),
                        e.getUser().getFullName() + " (" + e.getEmployeeNo() + ")"))
                .toList();
    }

    @Override
    @Transactional
    public AttendanceResponse checkIn(Long projectId) {
        Employee me = currentEmployee.requireEmployee();
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        lock.ensureOpen(today);
        Project project = access.requireParticipant(projectId);
        requireOnProject(me, project);
        if (attendanceRepository.existsByEmployeeIdAndDate(me.getId(), today)) {
            throw new DuplicateRecordException("attendance.checkin.done");
        }
        if (onApprovedLeave(me, today)) {
            throw new BusinessRuleException("attendance.onLeave");
        }
        Attendance attendance = attendanceRepository.save(Attendance.checkIn(me, project, now, currentUser.requireUser()));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.ATTENDANCE_CHECKED_IN)
                .entityId(attendance.getId()).args(project.getName()).build());
        return toResponse(attendance);
    }

    @Override
    @Transactional
    public AttendanceResponse checkOut() {
        Employee me = currentEmployee.requireEmployee();
        LocalDateTime now = LocalDateTime.now(clock);
        lock.ensureOpen(now.toLocalDate());
        Attendance attendance = attendanceRepository.findByEmployeeIdAndDate(me.getId(), now.toLocalDate())
                .orElseThrow(() -> new BusinessRuleException("attendance.checkout.noCheckIn"));
        attendance.checkOut(now);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.ATTENDANCE_CHECKED_OUT)
                .entityId(attendance.getId()).args(attendance.getProject().getName()).build());
        return toResponse(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> mine(YearMonth month) {
        Employee me = currentEmployee.requireEmployee();
        return attendanceRepository.findForEmployee(me.getId(), month.atDay(1), month.atEndOfMonth()).stream()
                .map(this::toResponse).toList();
    }

    // ----- site engineer / HR ------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> search(Long projectId, LocalDate from, LocalDate to, Boolean verified,
                                                   PageQuery pageQuery) {
        if (projectId != null) {
            viewableProject(projectId);
        }
        if (from != null && to != null && to.isBefore(from)) {
            throw BusinessRuleException.forField("to", "daterange.invalid");
        }
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        String search = pageQuery.getSearch();
        Specification<Attendance> spec = (root, q, cb) -> null;
        if (!SEE_ALL.contains(me.getRole())) {
            spec = spec.and(access.<Attendance>visibleThrough("project"));
        }
        spec = spec.and((root, q, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId))
                .and((root, q, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("date"), from))
                .and((root, q, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("date"), to))
                .and((root, q, cb) -> verified == null ? null : cb.equal(root.get("verified"), verified))
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
        return PageResponse.from(attendanceRepository.findAll(spec, pageQuery.toPageable(SORTS, "date", Sort.Direction.DESC)),
                this::toResponse);
    }

    @Override
    @Transactional
    public AttendanceResponse mark(AttendanceRequest request) {
        lock.ensureOpen(request.getDate());
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> BusinessRuleException.forField("employeeId", "attendance.employee.unknown"));
        Project project = requireMarkingRights(request.getProjectId(), employee);
        if (attendanceRepository.existsByEmployeeIdAndDate(employee.getId(), request.getDate())) {
            throw DuplicateRecordException.forField("date", "attendance.duplicate");
        }
        Attendance attendance = new Attendance(employee, project, request.getDate(), currentUser.requireUser());
        apply(attendance, project, employee, request);
        attendanceRepository.save(attendance);
        attendance.verify(currentUser.requireUser(), LocalDateTime.now(clock));
        saveOvertime(attendance, request);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.ATTENDANCE_MARKED)
                .entityId(attendance.getId())
                .args(employee.getUser().getFullName(), request.getDate(), attendance.getStatus())
                .build());
        return toResponse(attendance);
    }

    @Override
    @Transactional
    public AttendanceResponse correct(Long id, AttendanceRequest request) {
        Attendance attendance = find(id);
        lock.ensureOpen(attendance.getDate());
        if (!attendance.getDate().equals(request.getDate())
                || !attendance.getEmployee().getId().equals(request.getEmployeeId())) {
            throw BusinessRuleException.forField("date", "attendance.identity.fixed");
        }
        Employee employee = attendance.getEmployee();
        requireMarkingRights(attendance.getProject().getId(), employee);
        Project project = requireMarkingRights(request.getProjectId(), employee);
        apply(attendance, project, employee, request);
        attendance.verify(currentUser.requireUser(), LocalDateTime.now(clock));
        saveOvertime(attendance, request);
        WorkflowEvent.Builder event = WorkflowEvent.byCurrentUser(WorkflowEventType.ATTENDANCE_CORRECTED)
                .entityId(id).args(employee.getUser().getFullName(), attendance.getDate(), attendance.getStatus())
                .link("my-attendance");
        if (!employee.getUser().getId().equals(currentUser.requireId())) {
            event.recipients(employee.getUser().getId());
        }
        eventPublisher.publish(event.build());
        return toResponse(attendance);
    }

    @Override
    @Transactional
    public AttendanceResponse verify(Long id) {
        Attendance attendance = find(id);
        lock.ensureOpen(attendance.getDate());
        requireMarkingRights(attendance.getProject().getId(), attendance.getEmployee());
        attendance.verify(currentUser.requireUser(), LocalDateTime.now(clock));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.ATTENDANCE_VERIFIED)
                .entityId(id).args(attendance.getEmployee().getUser().getFullName(), attendance.getDate()).build());
        return toResponse(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceSheetRow> sheet(YearMonth month, Long projectId) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        if (projectId != null) {
            viewableProject(projectId);
        } else if (!SEE_ALL.contains(me.getRole())) {
            throw BusinessRuleException.forField("projectId", "attendance.sheet.projectRequired");
        }
        Map<Long, List<Attendance>> byEmployee = new LinkedHashMap<>();
        attendanceRepository.findBetween(month.atDay(1), month.atEndOfMonth(), projectId).stream()
                .sorted(Comparator.comparing(a -> a.getEmployee().getEmployeeNo()))
                .forEach(a -> byEmployee.computeIfAbsent(a.getEmployee().getId(), k -> new ArrayList<>()).add(a));
        List<AttendanceSheetRow> rows = new ArrayList<>();
        byEmployee.forEach((employeeId, records) -> {
            Employee e = records.get(0).getEmployee();
            Map<Integer, String> days = new LinkedHashMap<>();
            int[] counts = new int[5];
            for (Attendance a : records) {
                String code = switch (a.getStatus()) {
                    case PRESENT -> { counts[0]++; yield "P"; }
                    case HALF_DAY -> { counts[1]++; yield "H"; }
                    case ABSENT -> { counts[2]++; yield "A"; }
                    case ON_LEAVE -> { counts[3]++; yield "L"; }
                };
                if (!a.isVerified()) {
                    counts[4]++;
                }
                days.put(a.getDate().getDayOfMonth(), a.isVerified() ? code : code.toLowerCase(Locale.ROOT));
            }
            rows.add(new AttendanceSheetRow(employeeId, e.getEmployeeNo(), e.getUser().getFullName(), days,
                    counts[0], counts[1], counts[2], counts[3], counts[4]));
        });
        return rows;
    }

    // ----- overtime ---------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OvertimeResponse> overtime(RequestStatus status, PageQuery pageQuery) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        Specification<Overtime> spec = (root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status);
        if (!SEE_ALL.contains(me.getRole())) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("recordedBy").get("id"), me.getUserId()));
        }
        return PageResponse.from(overtimeRepository.findAll(spec, pageQuery.toPageable(OT_SORTS, "createdAt", Sort.Direction.DESC)),
                AttendanceServiceImpl::toOvertimeResponse);
    }

    @Override
    @Transactional
    public OvertimeResponse decideOvertime(Long id, boolean approve, String note) {
        if (currentUser.requirePrincipal().getRole() != RoleName.HR_MANAGER) {
            throw new UnauthorizedActionException("overtime.hrOnly");
        }
        Overtime overtime = overtimeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Overtime"));
        Attendance attendance = overtime.getAttendance();
        lock.ensureOpen(attendance.getDate());
        if (Objects.equals(attendance.getEmployee().getUser().getId(), currentUser.requireId())) {
            throw new UnauthorizedActionException("request.approve.own");
        }
        overtime.decide(approve, currentUser.requireUser(), note, LocalDateTime.now(clock));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(approve ? WorkflowEventType.OVERTIME_APPROVED
                        : WorkflowEventType.OVERTIME_REJECTED)
                .entityId(id).args(attendance.getEmployee().getUser().getFullName(), overtime.getHours(),
                        attendance.getDate(), note == null ? "" : note)
                .recipients(attendance.getEmployee().getUser().getId()).link("my-attendance").build());
        return toOvertimeResponse(overtime);
    }

    // ----- helpers -----------------------------------------------------------------------

    private void apply(Attendance attendance, Project project, Employee employee, AttendanceRequest r) {
        if (WORKING.contains(r.getStatus()) && onApprovedLeave(employee, r.getDate())) {
            throw BusinessRuleException.forField("status", "attendance.onLeave");
        }
        Shift shift = r.getShiftId() == null ? null : shiftRepository.findById(r.getShiftId())
                .orElseThrow(() -> BusinessRuleException.forField("shiftId", "shift.unknown"));
        LocalDateTime in = r.getCheckIn() == null ? null : r.getDate().atTime(r.getCheckIn());
        LocalDateTime out = r.getCheckOut() == null ? null : r.getDate().atTime(r.getCheckOut());
        if (out != null && in == null) {
            throw BusinessRuleException.forField("checkIn", "attendance.checkin.required");
        }
        attendance.record(project, shift, r.getStatus(), in, out, r.getNotes(), currentUser.requireUser());
    }

    /** Creates, updates or removes the overtime that goes with a record. */
    private void saveOvertime(Attendance attendance, AttendanceRequest r) {
        BigDecimal hours = r.getOvertimeHours();
        Optional<Overtime> existing = overtimeRepository.findByAttendanceId(attendance.getId());
        if (hours == null || hours.signum() == 0) {
            existing.filter(o -> o.getStatus() == RequestStatus.PENDING).ifPresent(overtimeRepository::delete);
            return;
        }
        Overtime overtime = existing.orElseGet(() -> new Overtime(attendance, currentUser.requireUser()));
        overtime.update(hours, r.getOvertimeReason(), currentUser.requireUser());
        overtimeRepository.save(overtime);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.OVERTIME_RECORDED)
                .entityId(overtime.getId())
                .args(attendance.getEmployee().getUser().getFullName(), hours, attendance.getDate())
                .recipients(userRepository.findActiveIdsByRole(RoleName.HR_MANAGER))
                .link("overtime").build());
    }

    /**
     * HR may mark anyone (on a project they belong to). A site engineer may mark
     * team members of their own projects, except themselves.
     */
    private Project requireMarkingRights(Long projectId, Employee employee) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        if (me.getRole() == RoleName.HR_MANAGER) {
            // HR is not a project member but keeps attendance for every site.
            Project project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Project"));
            project.ensureEditable();
            requireOnProject(employee, project);
            return project;
        }
        Project project = access.requireParticipant(projectId);
        requireOnProject(employee, project);
        if (me.getRole() == RoleName.SITE_ENGINEER && access.isMember(project)
                && !employee.getUser().getId().equals(me.getUserId())) {
            return project;
        }
        throw new UnauthorizedActionException("attendance.mark.denied");
    }

    /** HR, Admin and the Accountant may look at any site; others only at their own projects. */
    private Project viewableProject(Long projectId) {
        if (SEE_ALL.contains(currentUser.requirePrincipal().getRole())) {
            return projectRepository.findById(projectId)
                    .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Project"));
        }
        return access.requireVisible(projectId);
    }

    private void requireOnProject(Employee employee, Project project) {
        if (!memberRepository.existsByProjectIdAndUserId(project.getId(), employee.getUser().getId())) {
            throw BusinessRuleException.forField("projectId", "attendance.notOnProject");
        }
    }

    private boolean onApprovedLeave(Employee employee, LocalDate date) {
        return !leaveRepository.findApprovedOverlapping(employee.getId(), date, date).isEmpty();
    }

    private Attendance find(Long id) {
        return attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Attendance"));
    }

    private AttendanceResponse toResponse(Attendance a) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        boolean locked = lock.isLocked(a.getDate());
        boolean ownRecord = a.getEmployee().getUser().getId().equals(me.getUserId());
        boolean canMark = !locked && (me.getRole() == RoleName.HR_MANAGER
                || (me.getRole() == RoleName.SITE_ENGINEER && !ownRecord && access.isMember(a.getProject())));
        Optional<Overtime> overtime = overtimeRepository.findByAttendanceId(a.getId());
        Employee e = a.getEmployee();
        return new AttendanceResponse(a.getId(), e.getId(), e.getEmployeeNo(), e.getUser().getFullName(), e.getTrade(),
                a.getProject().getId(), a.getProject().getName(), a.getShift() == null ? null : a.getShift().getId(),
                a.getShift() == null ? null : a.getShift().getName(), a.getDate(), a.getStatus().name(), a.getCheckIn(),
                a.getCheckOut(), a.getHoursWorked(), a.isVerified(),
                a.getVerifiedBy() == null ? null : a.getVerifiedBy().getFullName(), a.getMarkedBy().getFullName(),
                a.getNotes(), overtime.map(Overtime::getHours).orElse(null),
                overtime.map(o -> o.getStatus().name()).orElse(null), locked, canMark && !a.isVerified(), canMark);
    }

    private static OvertimeResponse toOvertimeResponse(Overtime o) {
        Attendance a = o.getAttendance();
        User decider = o.getDecidedBy();
        return new OvertimeResponse(o.getId(), a.getId(), a.getEmployee().getUser().getFullName(),
                a.getEmployee().getEmployeeNo(), a.getProject().getName(), a.getDate(), o.getHours(), o.getReason(),
                o.getStatus().name(), o.getRecordedBy().getFullName(), a.isVerified(),
                decider == null ? null : decider.getFullName(), o.getDecidedAt(), o.getDecisionNote());
    }
}
