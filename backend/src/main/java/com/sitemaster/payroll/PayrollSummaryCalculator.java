package com.sitemaster.payroll;

import com.sitemaster.attendance.AttendanceRepository;
import com.sitemaster.attendance.OvertimeRepository;
import com.sitemaster.common.enums.AttendanceStatus;
import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.employee.Employee;
import com.sitemaster.leave.LeaveRequest;
import com.sitemaster.leave.LeaveRequestRepository;
import com.sitemaster.settings.WorkCalendar;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Works out HR's monthly attendance figures: verified attendance only, approved
 * leave (working days in the month, split paid/unpaid) and approved overtime on
 * verified days. Also decides who is on the payroll for a month.
 * <p>
 * OOP concept: Encapsulation - all payroll counting rules live here.
 */
@Component
public class PayrollSummaryCalculator {

    private final AttendanceRepository attendanceRepository;
    private final OvertimeRepository overtimeRepository;
    private final LeaveRequestRepository leaveRepository;
    private final WorkCalendar calendar;

    public PayrollSummaryCalculator(AttendanceRepository attendanceRepository, OvertimeRepository overtimeRepository,
                                    LeaveRequestRepository leaveRepository, WorkCalendar calendar) {
        this.attendanceRepository = attendanceRepository;
        this.overtimeRepository = overtimeRepository;
        this.leaveRepository = leaveRepository;
        this.calendar = calendar;
    }

    /** Contract staff are only paid in months their contract covers. */
    public boolean isPayable(Employee employee, YearMonth month) {
        if (employee.getEmploymentType() != EmploymentType.CONTRACT) {
            return true;
        }
        LocalDate start = employee.getContractStartDate();
        LocalDate end = employee.getContractEndDate();
        return start != null && end != null && !start.isAfter(month.atEndOfMonth()) && !end.isBefore(month.atDay(1));
    }

    public AttendanceTotals totals(Employee employee, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        int present = (int) attendanceRepository.countVerified(employee.getId(), AttendanceStatus.PRESENT, from, to);
        int half = (int) attendanceRepository.countVerified(employee.getId(), AttendanceStatus.HALF_DAY, from, to);
        int absent = (int) attendanceRepository.countVerified(employee.getId(), AttendanceStatus.ABSENT, from, to);
        BigDecimal paidLeave = BigDecimal.ZERO;
        BigDecimal unpaidLeave = BigDecimal.ZERO;
        List<LeaveRequest> leave = leaveRepository.findApprovedOverlapping(employee.getId(), from, to);
        for (LeaveRequest request : leave) {
            LocalDate start = request.getFromDate().isBefore(from) ? from : request.getFromDate();
            LocalDate end = request.getToDate().isAfter(to) ? to : request.getToDate();
            BigDecimal days = BigDecimal.valueOf(calendar.workingDaysBetween(start, end));
            if (request.getLeaveType().isPaid()) {
                paidLeave = paidLeave.add(days);
            } else {
                unpaidLeave = unpaidLeave.add(days);
            }
        }
        BigDecimal overtime = overtimeRepository.sumHours(employee.getId(), RequestStatus.APPROVED, from, to);
        return new AttendanceTotals(present, half, absent, paidLeave, unpaidLeave, overtime);
    }
}
