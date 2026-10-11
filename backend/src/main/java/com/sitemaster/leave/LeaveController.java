package com.sitemaster.leave;

import com.sitemaster.common.dto.MessageResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.dailyreport.dto.ReviewDecisionRequest;
import com.sitemaster.leave.dto.LeaveApplication;
import com.sitemaster.leave.dto.LeaveBalanceResponse;
import com.sitemaster.leave.dto.LeaveRequestResponse;
import com.sitemaster.leave.dto.LeaveTypeRequest;
import com.sitemaster.leave.dto.LeaveTypeResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Leave types, balances and requests. */
@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('LEAVE:VIEW')")
    public List<LeaveTypeResponse> types(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return leaveService.types(activeOnly);
    }

    @PostMapping("/types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LEAVE:EDIT')")
    public LeaveTypeResponse createType(@Valid @RequestBody LeaveTypeRequest request) {
        return leaveService.createType(request);
    }

    @PutMapping("/types/{id}")
    @PreAuthorize("hasAuthority('LEAVE:EDIT')")
    public LeaveTypeResponse updateType(@PathVariable Long id, @Valid @RequestBody LeaveTypeRequest request) {
        return leaveService.updateType(id, request);
    }

    @GetMapping("/balances/mine")
    @PreAuthorize("hasAuthority('LEAVE:CREATE')")
    public List<LeaveBalanceResponse> myBalances(@RequestParam @Min(value = 2000, message = "{year.range}") @Max(value = 2100, message = "{year.range}") int year) {
        return leaveService.myBalances(year);
    }

    @GetMapping("/balances")
    @PreAuthorize("hasAuthority('LEAVE:APPROVE')")
    public List<LeaveBalanceResponse> balances(@RequestParam Long employeeId,
                                               @RequestParam @Min(value = 2000, message = "{year.range}") @Max(value = 2100, message = "{year.range}") int year) {
        return leaveService.balances(employeeId, year);
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('LEAVE:CREATE')")
    public LeaveRequestResponse apply(@Valid @RequestBody LeaveApplication application) {
        return leaveService.apply(application);
    }

    @GetMapping("/requests/mine")
    @PreAuthorize("hasAuthority('LEAVE:CREATE')")
    public PageResponse<LeaveRequestResponse> mine(@Valid @ModelAttribute PageQuery pageQuery) {
        return leaveService.mine(pageQuery);
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAuthority('LEAVE:APPROVE') or hasAuthority('LEAVE:VIEW') and hasAnyRole('ADMIN', 'PROJECT_MANAGER')")
    public PageResponse<LeaveRequestResponse> search(@RequestParam(required = false) RequestStatus status,
                                                     @RequestParam(required = false) Long employeeId,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                     @Valid @ModelAttribute PageQuery pageQuery) {
        return leaveService.search(status, employeeId, from, to, pageQuery);
    }

    @PostMapping("/requests/{id}/approve")
    @PreAuthorize("hasAuthority('LEAVE:APPROVE')")
    public LeaveRequestResponse approve(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return leaveService.approve(id, request.getRemarks());
    }

    @PostMapping("/requests/{id}/reject")
    @PreAuthorize("hasAuthority('LEAVE:APPROVE')")
    public LeaveRequestResponse reject(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return leaveService.reject(id, request.getRemarks());
    }

    @DeleteMapping("/requests/{id}")
    @PreAuthorize("hasAuthority('LEAVE:CREATE')")
    public MessageResponse cancel(@PathVariable Long id) {
        leaveService.cancel(id);
        return new MessageResponse("Leave request cancelled.");
    }
}
