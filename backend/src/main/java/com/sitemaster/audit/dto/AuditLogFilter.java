package com.sitemaster.audit.dto;

import com.sitemaster.common.enums.AppModule;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query parameters of the audit log list (bound with @ModelAttribute).
 * <p>
 * OOP concept: Encapsulation - private fields with explicit accessors.
 */
public class AuditLogFilter {

    private Long userId;
    private AppModule module;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public AppModule getModule() {
        return module;
    }

    public void setModule(AppModule module) {
        this.module = module;
    }

    public LocalDate getFrom() {
        return from;
    }

    public void setFrom(LocalDate from) {
        this.from = from;
    }

    public LocalDate getTo() {
        return to;
    }

    public void setTo(LocalDate to) {
        this.to = to;
    }
}
