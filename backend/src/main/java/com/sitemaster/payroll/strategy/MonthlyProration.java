package com.sitemaster.payroll.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Shared arithmetic for strategies paid by the month. */
final class MonthlyProration {

    static final int SCALE = 2;

    private MonthlyProration() {
        // utility class
    }

    /**
     * monthly amount x paid days / working days, where paid days are the working
     * days minus recorded unpaid days (absence, unpaid leave, half of half days).
     */
    static BigDecimal prorate(BigDecimal monthly, SalaryContext context) {
        if (context.workingDays() == 0) {
            return BigDecimal.ZERO.setScale(SCALE);
        }
        BigDecimal ratio = paidDays(context)
                .divide(BigDecimal.valueOf(context.workingDays()), 6, RoundingMode.HALF_UP)
                .min(BigDecimal.ONE);
        return monthly.multiply(ratio).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** Working days minus recorded unpaid days, never below zero. */
    static BigDecimal paidDays(SalaryContext context) {
        return BigDecimal.valueOf(context.workingDays()).subtract(context.totals().unpaidDays()).max(BigDecimal.ZERO);
    }

    /** monthly amount / (working days x hours per day). */
    static BigDecimal hourly(BigDecimal monthly, SalaryContext context) {
        BigDecimal hours = BigDecimal.valueOf(Math.max(context.workingDays(), 1)).multiply(context.hoursPerDay());
        return monthly.divide(hours, SCALE, RoundingMode.HALF_UP);
    }
}
