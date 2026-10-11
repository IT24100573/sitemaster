package com.sitemaster.payroll.strategy;

// PATTERN: Strategy

import com.sitemaster.common.enums.EmploymentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Strategy pattern - CONCRETE STRATEGY for daily-wage workers: paid only for the
 * days actually worked (a half day earns half the daily rate); leave is unpaid.
 */
@Component
public class DailyWageSalaryStrategy implements SalaryCalculationStrategy {

    @Override
    public EmploymentType supports() {
        return EmploymentType.DAILY_WAGE;
    }

    @Override
    public BigDecimal basePay(SalaryContext context) {
        return context.employee().getDailyRate().multiply(context.totals().workedDays())
                .setScale(MonthlyProration.SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal hourlyRate(SalaryContext context) {
        return context.employee().getDailyRate().divide(context.hoursPerDay(), MonthlyProration.SCALE, RoundingMode.HALF_UP);
    }

    @Override
    public String describe(SalaryContext context) {
        return "Daily wage: " + context.totals().workedDays().stripTrailingZeros().toPlainString()
                + " day(s) x " + context.employee().getDailyRate().toPlainString();
    }
}
