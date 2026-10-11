package com.sitemaster.leave.dto;

import com.sitemaster.validation.ValidDateRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * An employee applies for leave. Past dates are refused except for leave types
 * that allow back-dating (sick leave, up to 3 days) - checked in the service.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
@ValidDateRange(start = "fromDate", end = "toDate")
public class LeaveApplication {

    @NotNull(message = "{field.required}")
    private Long leaveTypeId;

    @NotNull(message = "{field.required}")
    private LocalDate fromDate;

    @NotNull(message = "{field.required}")
    private LocalDate toDate;

    @NotBlank(message = "{field.required}")
    @Size(min = 5, max = 500, message = "{reason.size}")
    private String reason;

    public Long getLeaveTypeId() {
        return leaveTypeId;
    }

    public void setLeaveTypeId(Long leaveTypeId) {
        this.leaveTypeId = leaveTypeId;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
