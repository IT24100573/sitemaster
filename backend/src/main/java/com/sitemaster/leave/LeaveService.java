package com.sitemaster.leave;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.leave.dto.LeaveApplication;
import com.sitemaster.leave.dto.LeaveBalanceResponse;
import com.sitemaster.leave.dto.LeaveRequestResponse;
import com.sitemaster.leave.dto.LeaveTypeRequest;
import com.sitemaster.leave.dto.LeaveTypeResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Leave types, balances and requests (apply, approve, reject, cancel).
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface LeaveService {

    List<LeaveTypeResponse> types(boolean activeOnly);

    LeaveTypeResponse createType(LeaveTypeRequest request);

    LeaveTypeResponse updateType(Long id, LeaveTypeRequest request);

    List<LeaveBalanceResponse> myBalances(int year);

    List<LeaveBalanceResponse> balances(Long employeeId, int year);

    LeaveRequestResponse apply(LeaveApplication application);

    PageResponse<LeaveRequestResponse> mine(PageQuery pageQuery);

    PageResponse<LeaveRequestResponse> search(RequestStatus status, Long employeeId, LocalDate from, LocalDate to,
                                              PageQuery pageQuery);

    LeaveRequestResponse approve(Long id, String remarks);

    LeaveRequestResponse reject(Long id, String remarks);

    void cancel(Long id);
}
