package com.sitemaster.payroll;

import com.sitemaster.payroll.decorator.AllowanceDecorator;
import com.sitemaster.payroll.decorator.BasePay;
import com.sitemaster.payroll.decorator.DeductionDecorator;
import com.sitemaster.payroll.decorator.OvertimeDecorator;
import com.sitemaster.payroll.decorator.PayslipComponent;
import com.sitemaster.payroll.strategy.SalaryCalculationStrategy;
import com.sitemaster.payroll.strategy.SalaryContext;
import com.sitemaster.payroll.strategy.SalaryStrategyResolver;
import com.sitemaster.settings.CompanySettings;
import com.sitemaster.settings.SystemSettingsManager;
import com.sitemaster.settings.WorkCalendar;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Builds one payslip calculation:
 * <ol>
 *   <li>STRATEGY - the employee's employment type picks how base pay is worked out;</li>
 *   <li>DECORATOR - base pay is wrapped in overtime, allowance and EPF deduction
 *       layers; each layer adds one line and the outer layer holds the net pay.</li>
 * </ol>
 * All rates come from System Settings.
 */
@Component
public class PayslipCalculator {

    private final SalaryStrategyResolver strategies;
    private final WorkCalendar calendar;

    public PayslipCalculator(SalaryStrategyResolver strategies, WorkCalendar calendar) {
        this.strategies = strategies;
        this.calendar = calendar;
    }

    public PayslipComponent calculate(PayrollSummary summary) {
        CompanySettings settings = SystemSettingsManager.getInstance().getSettings();
        AttendanceTotals totals = summary.totals();
        SalaryContext context = new SalaryContext(summary.getEmployee(), summary.getPayrollRun().period(), totals,
                summary.getPayrollRun().getWorkingDays(), calendar.hoursPerDay());

        SalaryCalculationStrategy strategy = strategies.forType(summary.getEmploymentType());
        PayslipComponent payslip = new BasePay(strategy.describe(context), strategy.basePay(context));
        if (totals.overtimeHours().signum() > 0) {
            payslip = new OvertimeDecorator(payslip, totals.overtimeHours(), strategy.hourlyRate(context),
                    settings.overtimeMultiplier());
        }
        BigDecimal allowance = settings.siteAllowancePerDay();
        if (allowance != null && allowance.signum() > 0 && totals.workedDays().signum() > 0) {
            payslip = new AllowanceDecorator(payslip, totals.workedDays(), allowance);
        }
        if (settings.epfEmployeeRate() != null && settings.epfEmployeeRate().signum() > 0) {
            payslip = new DeductionDecorator(payslip, "EPF employee contribution", settings.epfEmployeeRate());
        }
        return payslip;
    }
}
