package com.sitemaster.payroll.strategy;

// PATTERN: Strategy

import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.employee.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Strategy pattern - CONCRETE STRATEGY for contract employees: the contract amount
 * is spread evenly over the months of the contract, then prorated for unpaid
 * days. Months outside the contract period pay nothing.
 */
@Component
public class ContractSalaryStrategy implements SalaryCalculationStrategy {

    @Override
    public EmploymentType supports() {
        return EmploymentType.CONTRACT;
    }

    @Override
    public BigDecimal basePay(SalaryContext context) {
        return MonthlyProration.prorate(monthlyShare(context), context);
    }

    @Override
    public BigDecimal hourlyRate(SalaryContext context) {
        return MonthlyProration.hourly(monthlyShare(context), context);
    }

    @Override
    public String describe(SalaryContext context) {
        return "Contract share " + monthlyShare(context).toPlainString() + " for "
                + MonthlyProration.paidDays(context).stripTrailingZeros().toPlainString() + " of "
                + context.workingDays() + " working day(s)";
    }

    /** Contract amount divided by the number of calendar months the contract touches. */
    BigDecimal monthlyShare(SalaryContext context) {
        Employee e = context.employee();
        YearMonth first = YearMonth.from(e.getContractStartDate());
        YearMonth last = YearMonth.from(e.getContractEndDate());
        if (context.period().isBefore(first) || context.period().isAfter(last)) {
            return BigDecimal.ZERO.setScale(MonthlyProration.SCALE);
        }
        long months = ChronoUnit.MONTHS.between(first, last) + 1;
        return e.getContractAmount().divide(BigDecimal.valueOf(months), MonthlyProration.SCALE, RoundingMode.HALF_UP);
    }
}
