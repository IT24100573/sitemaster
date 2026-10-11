package com.sitemaster.payroll;

import java.math.BigDecimal;

/**
 * Attendance figures for one employee and month.
 *
 * @param presentDays     verified PRESENT days
 * @param halfDays        verified HALF_DAY days
 * @param absentDays      verified ABSENT days
 * @param paidLeaveDays   approved paid-leave working days in the month
 * @param unpaidLeaveDays approved unpaid-leave working days in the month
 * @param overtimeHours   approved overtime on verified days
 */
public record AttendanceTotals(int presentDays, int halfDays, int absentDays, BigDecimal paidLeaveDays,
                               BigDecimal unpaidLeaveDays, BigDecimal overtimeHours) {

    /** Days that earn pay: present + half of half days + paid leave. */
    public BigDecimal payableDays() {
        return BigDecimal.valueOf(presentDays)
                .add(BigDecimal.valueOf(halfDays).multiply(new BigDecimal("0.5")))
                .add(paidLeaveDays);
    }

    /**
     * Recorded unpaid days: verified absences, unpaid leave and half of each half
     * day. Salaried staff are paid for every working day except these.
     */
    public BigDecimal unpaidDays() {
        return BigDecimal.valueOf(absentDays)
                .add(unpaidLeaveDays)
                .add(BigDecimal.valueOf(halfDays).multiply(new BigDecimal("0.5")));
    }

    /** Days actually worked on site: present + half of half days. */
    public BigDecimal workedDays() {
        return BigDecimal.valueOf(presentDays).add(BigDecimal.valueOf(halfDays).multiply(new BigDecimal("0.5")));
    }
}
