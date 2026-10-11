package com.sitemaster.attendance.dto;

import com.sitemaster.common.enums.AttendanceStatus;
import com.sitemaster.validation.NotFutureDate;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A site engineer or HR marks or corrects a day's attendance, optionally with
 * overtime hours (0-8) that HR then approves.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class AttendanceRequest {

    @NotNull(message = "{field.required}")
    private Long employeeId;

    @NotNull(message = "{field.required}")
    private Long projectId;

    @NotNull(message = "{field.required}")
    @NotFutureDate
    private LocalDate date;

    @NotNull(message = "{field.required}")
    private AttendanceStatus status;

    private Long shiftId;

    private LocalTime checkIn;

    private LocalTime checkOut;

    @DecimalMin(value = "0", message = "{overtime.hours.range}")
    @DecimalMax(value = "8", message = "{overtime.hours.range}")
    @Digits(integer = 1, fraction = 2, message = "{overtime.hours.range}")
    private BigDecimal overtimeHours;

    @Size(max = 500, message = "{note.size}")
    private String overtimeReason;

    @Size(max = 500, message = "{note.size}")
    private String notes;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public Long getShiftId() {
        return shiftId;
    }

    public void setShiftId(Long shiftId) {
        this.shiftId = shiftId;
    }

    public LocalTime getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalTime checkIn) {
        this.checkIn = checkIn;
    }

    public LocalTime getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalTime checkOut) {
        this.checkOut = checkOut;
    }

    public BigDecimal getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(BigDecimal overtimeHours) {
        this.overtimeHours = overtimeHours;
    }

    public String getOvertimeReason() {
        return overtimeReason;
    }

    public void setOvertimeReason(String overtimeReason) {
        this.overtimeReason = overtimeReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
