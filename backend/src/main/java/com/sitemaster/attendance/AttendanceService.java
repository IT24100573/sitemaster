package com.sitemaster.attendance;

import com.sitemaster.attendance.dto.AttendanceRequest;
import com.sitemaster.attendance.dto.AttendanceResponse;
import com.sitemaster.attendance.dto.AttendanceSheetRow;
import com.sitemaster.attendance.dto.OvertimeResponse;
import com.sitemaster.attendance.dto.TodayResponse;
import com.sitemaster.common.dto.LookupOption;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RequestStatus;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Attendance and overtime: self check-in/out, marking and verification by site
 * engineers, corrections by HR, overtime approval and the monthly sheet.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface AttendanceService {

    TodayResponse today();

    /** Employees on a project's team, for the marking form. */
    List<LookupOption> team(Long projectId);

    AttendanceResponse checkIn(Long projectId);

    AttendanceResponse checkOut();

    List<AttendanceResponse> mine(YearMonth month);

    PageResponse<AttendanceResponse> search(Long projectId, LocalDate from, LocalDate to, Boolean verified,
                                            PageQuery pageQuery);

    AttendanceResponse mark(AttendanceRequest request);

    AttendanceResponse correct(Long id, AttendanceRequest request);

    AttendanceResponse verify(Long id);

    List<AttendanceSheetRow> sheet(YearMonth month, Long projectId);

    PageResponse<OvertimeResponse> overtime(RequestStatus status, PageQuery pageQuery);

    OvertimeResponse decideOvertime(Long id, boolean approve, String note);
}
