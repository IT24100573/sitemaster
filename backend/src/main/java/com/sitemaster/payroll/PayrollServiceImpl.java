package com.sitemaster.payroll;

import com.sitemaster.attendance.AttendanceRepository;
import com.sitemaster.attendance.OvertimeRepository;
import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.common.enums.PayrollStatus;
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
import com.sitemaster.payroll.decorator.PayslipComponent;
import com.sitemaster.payroll.decorator.PayslipEntry;
import com.sitemaster.payroll.dto.PayrollPreviewResponse;
import com.sitemaster.payroll.dto.PayrollRunDetail;
import com.sitemaster.payroll.dto.PayrollRunResponse;
import com.sitemaster.payroll.dto.PayslipResponse;
import com.sitemaster.payroll.dto.SummaryRow;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.settings.WorkCalendar;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Implementation of {@link PayrollService}. Rules:
 * <ul>
 *   <li>one run per month, never for a future month;</li>
 *   <li>HR's summary must be sent before calculation; sending locks the month's attendance;</li>
 *   <li>only verified attendance and approved overtime/leave count;</li>
 *   <li>net pay cannot be negative; a payslip is paid once; a PAID run is read-only;</li>
 *   <li>employees see only their own payslips.</li>
 * </ul>
 * OOP concept: Abstraction - implements PayrollService.
 */
@Service
public class PayrollServiceImpl implements PayrollService {

    private static final Set<RoleName> PAYROLL_STAFF =
            EnumSet.of(RoleName.ADMIN, RoleName.HR_MANAGER, RoleName.ACCOUNTANT);

    private final PayrollRunRepository runRepository;
    private final PayrollSummaryRepository summaryRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final OvertimeRepository overtimeRepository;
    private final LeaveRequestRepository leaveRepository;
    private final UserRepository userRepository;
    private final PayrollSummaryCalculator summaryCalculator;
    private final PayslipCalculator payslipCalculator;
    private final PayslipPdfGenerator pdfGenerator;
    private final WorkCalendar calendar;
    private final CurrentUserProvider currentUser;
    private final CurrentEmployeeProvider currentEmployee;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public PayrollServiceImpl(PayrollRunRepository runRepository, PayrollSummaryRepository summaryRepository,
                              PayslipRepository payslipRepository, EmployeeRepository employeeRepository,
                              AttendanceRepository attendanceRepository, OvertimeRepository overtimeRepository,
                              LeaveRequestRepository leaveRepository, UserRepository userRepository,
                              PayrollSummaryCalculator summaryCalculator, PayslipCalculator payslipCalculator,
                              PayslipPdfGenerator pdfGenerator, WorkCalendar calendar, CurrentUserProvider currentUser,
                              CurrentEmployeeProvider currentEmployee, WorkflowEventPublisher eventPublisher,
                              Clock clock) {
        this.runRepository = runRepository;
        this.summaryRepository = summaryRepository;
        this.payslipRepository = payslipRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.overtimeRepository = overtimeRepository;
        this.leaveRepository = leaveRepository;
        this.userRepository = userRepository;
        this.summaryCalculator = summaryCalculator;
        this.payslipCalculator = payslipCalculator;
        this.pdfGenerator = pdfGenerator;
        this.calendar = calendar;
        this.currentUser = currentUser;
        this.currentEmployee = currentEmployee;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // ----- HR ------------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PayrollPreviewResponse preview(YearMonth month) {
        requireNotFuture(month);
        List<SummaryRow> rows = payable(month).stream()
                .map(e -> toRow(e, summaryCalculator.totals(e, month)))
                .toList();
        return new PayrollPreviewResponse(month.getYear(), month.getMonthValue(), calendar.workingDays(month),
                runRepository.existsByYearAndMonth(month.getYear(), month.getMonthValue()), rows,
                attendanceRepository.countByVerifiedFalseAndDateBetween(month.atDay(1), month.atEndOfMonth()),
                overtimeRepository.countByStatusAndAttendanceDateBetween(RequestStatus.PENDING, month.atDay(1),
                        month.atEndOfMonth()),
                leaveRepository.countPendingOverlapping(month.atDay(1), month.atEndOfMonth()));
    }

    @Override
    @Transactional
    public PayrollRunResponse sendSummary(YearMonth month) {
        requireRole(RoleName.HR_MANAGER);
        requireNotFuture(month);
        if (runRepository.existsByYearAndMonth(month.getYear(), month.getMonthValue())) {
            throw DuplicateRecordException.forField("month", "payroll.month.duplicate");
        }
        List<Employee> employees = payable(month);
        if (employees.isEmpty()) {
            throw BusinessRuleException.forField("month", "payroll.noEmployees");
        }
        PayrollRun run = runRepository.save(new PayrollRun(month, calendar.workingDays(month), currentUser.requireUser(),
                LocalDateTime.now(clock)));
        for (Employee employee : employees) {
            summaryRepository.save(new PayrollSummary(run, employee, summaryCalculator.totals(employee, month)));
        }
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PAYROLL_SUMMARY_SENT)
                .entityId(run.getId()).args(label(month), employees.size())
                .recipients(userRepository.findActiveIdsByRole(RoleName.ACCOUNTANT))
                .link("payroll/" + run.getId()).build());
        return toResponse(run);
    }

    // ----- Accountant -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<PayrollRunResponse> runs() {
        requireStaff();
        return runRepository.findAllByOrderByYearDescMonthDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollRunDetail run(Long id) {
        requireStaff();
        return detail(findRun(id));
    }

    @Override
    @Transactional
    public PayrollRunDetail calculate(Long id) {
        requireRole(RoleName.ACCOUNTANT);
        PayrollRun run = findRun(id);
        run.ensureCalculable();
        payslipRepository.deleteForRun(run.getId());
        payslipRepository.flush();

        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;
        BigDecimal net = BigDecimal.ZERO;
        for (PayrollSummary summary : summaryRepository.findForRun(run.getId())) {
            PayslipComponent result = payslipCalculator.calculate(summary);
            List<PayslipEntry> entries = result.entries();
            BigDecimal slipDeductions = entries.stream().filter(e -> e.type() == PayslipEntry.LineType.DEDUCTION)
                    .map(PayslipEntry::amount).reduce(BigDecimal.ZERO, BigDecimal::add).negate();
            BigDecimal slipGross = result.total().add(slipDeductions);
            Payslip payslip = new Payslip(run, summary.getEmployee(), slipGross, slipDeductions, result.total());
            for (int i = 0; i < entries.size(); i++) {
                payslip.addLine(new PayslipLine(payslip, i + 1, entries.get(i)));
            }
            payslipRepository.save(payslip);
            gross = gross.add(slipGross);
            deductions = deductions.add(slipDeductions);
            net = net.add(result.total());
        }
        run.markCalculated(currentUser.requireUser(), LocalDateTime.now(clock), gross, deductions, net);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PAYROLL_CALCULATED)
                .entityId(run.getId()).args(label(run.period()), net)
                .recipients(run.getSummarySentBy().getId()).link("payroll").build());
        return detail(run);
    }

    @Override
    @Transactional
    public PayrollRunDetail payAll(Long id, PaymentMethod method, String reference) {
        requireRole(RoleName.ACCOUNTANT);
        PayrollRun run = findRun(id);
        run.ensurePayable();
        LocalDateTime now = LocalDateTime.now(clock);
        for (Payslip payslip : payslipRepository.findForRun(run.getId())) {
            if (!payslip.isPaid()) {
                pay(payslip, method, reference, now);
            }
        }
        completeIfPaid(run, now);
        return detail(run);
    }

    @Override
    @Transactional
    public PayslipResponse paySlip(Long payslipId, PaymentMethod method, String reference) {
        requireRole(RoleName.ACCOUNTANT);
        Payslip payslip = findPayslip(payslipId);
        PayrollRun run = payslip.getPayrollRun();
        run.ensurePayable();
        LocalDateTime now = LocalDateTime.now(clock);
        pay(payslip, method, reference, now);
        payslipRepository.flush();
        completeIfPaid(run, now);
        return toResponse(payslip);
    }

    private void pay(Payslip payslip, PaymentMethod method, String reference, LocalDateTime now) {
        payslip.pay(method, reference, now);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PAYSLIP_PAID)
                .entityId(payslip.getId()).args(label(payslip.getPayrollRun().period()), payslip.getNetPay(),
                        payslip.getEmployee().getUser().getFullName())
                .recipients(payslip.getEmployee().getUser().getId()).link("payslips").build());
    }

    private void completeIfPaid(PayrollRun run, LocalDateTime now) {
        if (payslipRepository.countByPayrollRunIdAndPaidFalse(run.getId()) == 0) {
            run.markPaid(now);
            eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PAYROLL_PAID)
                    .entityId(run.getId()).args(label(run.period()), run.getTotalNet()).build());
        }
    }

    // ----- employees ----------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<PayslipResponse> myPayslips() {
        Employee me = currentEmployee.requireEmployee();
        return payslipRepository.findForEmployee(me.getId()).stream()
                .filter(p -> p.getPayrollRun().getStatus() != PayrollStatus.SUMMARY_SENT)
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayslipResponse payslip(Long id) {
        return toResponse(findVisiblePayslip(id));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] payslipPdf(Long id) {
        return pdfGenerator.generate(findVisiblePayslip(id));
    }

    // ----- helpers ---------------------------------------------------------------------------

    private List<Employee> payable(YearMonth month) {
        return employeeRepository.findPayable(month.atEndOfMonth()).stream()
                .filter(e -> summaryCalculator.isPayable(e, month))
                .toList();
    }

    private void requireNotFuture(YearMonth month) {
        if (month.isAfter(YearMonth.now(clock))) {
            throw BusinessRuleException.forField("month", "payroll.month.future");
        }
    }

    private void requireRole(RoleName role) {
        if (currentUser.requirePrincipal().getRole() != role) {
            throw new UnauthorizedActionException("payroll.role", role.name());
        }
    }

    private void requireStaff() {
        if (!PAYROLL_STAFF.contains(currentUser.requirePrincipal().getRole())) {
            throw new UnauthorizedActionException("payroll.staffOnly");
        }
    }

    /** Own payslip, or any payslip for payroll staff. */
    private Payslip findVisiblePayslip(Long id) {
        Payslip payslip = findPayslip(id);
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        boolean own = payslip.getEmployee().getUser().getId().equals(me.getUserId());
        if (!own && !PAYROLL_STAFF.contains(me.getRole())) {
            throw new ResourceNotFoundException("resource.notFound", "Payslip");
        }
        return payslip;
    }

    private PayrollRun findRun(Long id) {
        return runRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Payroll run"));
    }

    private Payslip findPayslip(Long id) {
        return payslipRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Payslip"));
    }

    private static String label(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " "
                + month.getYear();
    }

    private PayrollRunDetail detail(PayrollRun run) {
        List<SummaryRow> summaries = summaryRepository.findForRun(run.getId()).stream()
                .map(s -> toRow(s.getEmployee(), s.totals())).toList();
        List<PayslipResponse> payslips = payslipRepository.findForRun(run.getId()).stream().map(this::toResponse).toList();
        return new PayrollRunDetail(toResponse(run), summaries, payslips);
    }

    private PayrollRunResponse toResponse(PayrollRun r) {
        int employees = summaryRepository.findForRun(r.getId()).size();
        return new PayrollRunResponse(r.getId(), r.getYear(), r.getMonth(), r.getStatus().name(), r.getWorkingDays(),
                r.getSummarySentBy().getFullName(), r.getSummarySentAt(),
                r.getCalculatedBy() == null ? null : r.getCalculatedBy().getFullName(), r.getCalculatedAt(),
                r.getPaidAt(), r.getTotalGross(), r.getTotalDeductions(), r.getTotalNet(), employees,
                payslipRepository.countByPayrollRunIdAndPaidFalse(r.getId()));
    }

    private static SummaryRow toRow(Employee e, AttendanceTotals t) {
        return new SummaryRow(e.getId(), e.getEmployeeNo(), e.getUser().getFullName(), e.getEmploymentType().name(),
                t.presentDays(), t.halfDays(), t.absentDays(), t.paidLeaveDays(), t.unpaidLeaveDays(), t.overtimeHours());
    }

    private PayslipResponse toResponse(Payslip p) {
        PayrollRun run = p.getPayrollRun();
        Employee e = p.getEmployee();
        return new PayslipResponse(p.getId(), run.getId(), run.getYear(), run.getMonth(), e.getId(), e.getEmployeeNo(),
                e.getUser().getFullName(), p.getEmploymentType().name(), p.getGrossPay(), p.getTotalDeductions(),
                p.getNetPay(), p.isPaid(), p.getPaidAt(), p.getPaymentMethod() == null ? null : p.getPaymentMethod().name(),
                p.getPaymentReference(),
                p.getLines().stream().map(l -> new PayslipResponse.Line(l.getLineType(), l.getLabel(), l.getAmount(),
                        l.getRunningTotal())).toList());
    }
}
