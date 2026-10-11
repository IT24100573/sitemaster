package com.sitemaster.payroll;

import com.sitemaster.common.exception.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Once HR has sent a month's payroll summary, that month's attendance and
 * overtime are read-only. Every attendance change asks this component first.
 * <p>
 * OOP concept: Encapsulation - one place owns the "month is locked" rule.
 */
@Component
public class AttendanceLock {

    private final PayrollRunRepository runRepository;

    public AttendanceLock(PayrollRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public boolean isLocked(LocalDate date) {
        return runRepository.existsByYearAndMonth(date.getYear(), date.getMonthValue());
    }

    public void ensureOpen(LocalDate date) {
        if (isLocked(date)) {
            throw new BusinessRuleException("attendance.month.locked");
        }
    }
}
