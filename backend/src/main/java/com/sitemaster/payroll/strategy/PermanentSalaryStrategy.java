package com.sitemaster.payroll.strategy;

// PATTERN: Strategy

import com.sitemaster.common.enums.EmploymentType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Strategy pattern - CONCRETE STRATEGY for permanent staff: the full monthly
 * salary, reduced only for recorded unpaid days (verified absence, unpaid leave,
 * half of each half day). Office staff without site attendance are paid in full.
 */
@Component
public class PermanentSalaryStrategy implements SalaryCalculationStrategy {

    @Override
    public EmploymentType supports() {
        return EmploymentType.PERMANENT;
    }

    @Override
    public BigDecimal basePay(SalaryContext context) {
        return MonthlyProration.prorate(context.employee().getMonthlySalary(), context);
    }

    @Override
    public BigDecimal hourlyRate(SalaryContext context) {
        return MonthlyProration.hourly(context.employee().getMonthlySalary(), context);
    }

    @Override
    public String describe(SalaryContext context) {
        return "Monthly salary " + context.employee().getMonthlySalary().toPlainString() + " for "
                + MonthlyProration.paidDays(context).stripTrailingZeros().toPlainString() + " of "
                + context.workingDays() + " working day(s)";
    }
}
