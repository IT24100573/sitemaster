package com.sitemaster.attendance;

import com.sitemaster.attendance.dto.AttendanceRequest;
import com.sitemaster.attendance.dto.AttendanceResponse;
import com.sitemaster.attendance.dto.AttendanceSheetRow;
import com.sitemaster.attendance.dto.CheckInRequest;
import com.sitemaster.attendance.dto.OvertimeResponse;
import com.sitemaster.attendance.dto.ShiftRequest;
import com.sitemaster.attendance.dto.ShiftResponse;
import com.sitemaster.attendance.dto.TodayResponse;
import com.sitemaster.common.dto.LookupOption;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.dailyreport.dto.ReviewDecisionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Attendance, overtime and shifts. */
@RestController
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final ShiftService shiftService;

    public AttendanceController(AttendanceService attendanceService, ShiftService shiftService) {
        this.attendanceService = attendanceService;
        this.shiftService = shiftService;
    }

    // ----- self service --------------------------------------------------------------

    @GetMapping("/api/attendance/today")
    @PreAuthorize("hasAuthority('ATTENDANCE:CREATE')")
    public TodayResponse today() {
        return attendanceService.today();
    }

    @GetMapping("/api/attendance/team")
    @PreAuthorize("hasAuthority('ATTENDANCE:EDIT')")
    public List<LookupOption> team(@RequestParam Long projectId) {
        return attendanceService.team(projectId);
    }

    @PostMapping("/api/attendance/check-in")
    @PreAuthorize("hasAuthority('ATTENDANCE:CREATE')")
    public AttendanceResponse checkIn(@Valid @RequestBody CheckInRequest request) {
        return attendanceService.checkIn(request.getProjectId());
    }

    @PostMapping("/api/attendance/check-out")
    @PreAuthorize("hasAuthority('ATTENDANCE:CREATE')")
    public AttendanceResponse checkOut() {
        return attendanceService.checkOut();
    }

    @GetMapping("/api/attendance/mine")
    @PreAuthorize("hasAuthority('ATTENDANCE:VIEW')")
    public List<AttendanceResponse> mine(@RequestParam @Min(value = 2000, message = "{year.range}") @Max(value = 2100, message = "{year.range}") int year,
                                         @RequestParam @Min(value = 1, message = "{month.range}") @Max(value = 12, message = "{month.range}") int month) {
        return attendanceService.mine(YearMonth.of(year, month));
    }

    // ----- site engineer / HR ------------------------------------------------------------

    @GetMapping("/api/attendance")
    @PreAuthorize("hasAuthority('ATTENDANCE:VIEW')")
    public PageResponse<AttendanceResponse> list(@RequestParam(required = false) Long projectId,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                 @RequestParam(required = false) Boolean verified,
                                                 @Valid @ModelAttribute PageQuery pageQuery) {
        return attendanceService.search(projectId, from, to, verified, pageQuery);
    }

    @PostMapping("/api/attendance")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ATTENDANCE:EDIT')")
    public AttendanceResponse mark(@Valid @RequestBody AttendanceRequest request) {
        return attendanceService.mark(request);
    }

    @PutMapping("/api/attendance/{id}")
    @PreAuthorize("hasAuthority('ATTENDANCE:EDIT')")
    public AttendanceResponse correct(@PathVariable Long id, @Valid @RequestBody AttendanceRequest request) {
        return attendanceService.correct(id, request);
    }

    @PostMapping("/api/attendance/{id}/verify")
    @PreAuthorize("hasAuthority('ATTENDANCE:APPROVE')")
    public AttendanceResponse verify(@PathVariable Long id) {
        return attendanceService.verify(id);
    }

    @GetMapping("/api/attendance/sheet")
    @PreAuthorize("hasAuthority('ATTENDANCE:VIEW')")
    public List<AttendanceSheetRow> sheet(@RequestParam @Min(value = 2000, message = "{year.range}") @Max(value = 2100, message = "{year.range}") int year,
                                          @RequestParam @Min(value = 1, message = "{month.range}") @Max(value = 12, message = "{month.range}") int month,
                                          @RequestParam(required = false) Long projectId) {
        return attendanceService.sheet(YearMonth.of(year, month), projectId);
    }

    // ----- overtime ---------------------------------------------------------------------

    @GetMapping("/api/overtime")
    @PreAuthorize("hasAuthority('ATTENDANCE:APPROVE')")
    public PageResponse<OvertimeResponse> overtime(@RequestParam(required = false) RequestStatus status,
                                                   @Valid @ModelAttribute PageQuery pageQuery) {
        return attendanceService.overtime(status, pageQuery);
    }

    @PostMapping("/api/overtime/{id}/approve")
    @PreAuthorize("hasAuthority('ATTENDANCE:APPROVE')")
    public OvertimeResponse approveOvertime(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return attendanceService.decideOvertime(id, true, request.getRemarks());
    }

    @PostMapping("/api/overtime/{id}/reject")
    @PreAuthorize("hasAuthority('ATTENDANCE:APPROVE')")
    public OvertimeResponse rejectOvertime(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return attendanceService.decideOvertime(id, false, request.getRemarks());
    }

    // ----- shifts ------------------------------------------------------------------------

    @GetMapping("/api/shifts")
    @PreAuthorize("hasAuthority('ATTENDANCE:VIEW')")
    public List<ShiftResponse> shifts() {
        return shiftService.list();
    }

    @PostMapping("/api/shifts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EMPLOYEE:EDIT')")
    public ShiftResponse createShift(@Valid @RequestBody ShiftRequest request) {
        return shiftService.create(request);
    }

    @PutMapping("/api/shifts/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE:EDIT')")
    public ShiftResponse updateShift(@PathVariable Long id, @Valid @RequestBody ShiftRequest request) {
        return shiftService.update(id, request);
    }
}
