package com.sitemaster.hr.dto;

import com.sitemaster.validation.NotFutureDate;
import com.sitemaster.validation.ValidDateRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * HR records a completed training or safety certification.
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
@ValidDateRange(start = "completedOn", end = "expiresOn")
public class TrainingRequest {

    @NotNull(message = "{field.required}")
    private Long employeeId;

    @NotBlank(message = "{field.required}")
    @Size(min = 2, max = 150, message = "{training.title.size}")
    private String title;

    @Size(max = 150, message = "{training.provider.size}")
    private String provider;

    private boolean safety;

    @NotNull(message = "{field.required}")
    @NotFutureDate
    private LocalDate completedOn;

    private LocalDate expiresOn;

    @Size(max = 500, message = "{note.size}")
    private String notes;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isSafety() {
        return safety;
    }

    public void setSafety(boolean safety) {
        this.safety = safety;
    }

    public LocalDate getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(LocalDate completedOn) {
        this.completedOn = completedOn;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(LocalDate expiresOn) {
        this.expiresOn = expiresOn;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
