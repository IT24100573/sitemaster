package com.sitemaster.payroll;

import com.sitemaster.common.enums.RoleName;
import com.sitemaster.payroll.decorator.PayslipComponent;
import com.sitemaster.payroll.decorator.PayslipEntry;
import com.sitemaster.payroll.strategy.ContractSalaryStrategy;
import com.sitemaster.payroll.strategy.DailyWageSalaryStrategy;
import com.sitemaster.payroll.strategy.PermanentSalaryStrategy;
import com.sitemaster.payroll.strategy.SalaryStrategyResolver;
import com.sitemaster.settings.WorkCalendar;
import com.sitemaster.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Strategy and Decorator working together, with rates from System Settings. */
class PayslipCalculatorTest {

    private PayslipCalculator calculator;

    @BeforeEach
    void setUp() {
        TestData.loadSettings();
        calculator = new PayslipCalculator(new SalaryStrategyResolver(List.of(new DailyWageSalaryStrategy(),
                new PermanentSalaryStrategy(), new ContractSalaryStrategy())), new WorkCalendar());
    }

    @Test
    void dailyWageWorkerGetsBaseOvertimeAllowanceAndEpf() {
        PayrollRun run = new PayrollRun(YearMonth.of(2026, 9), 26, TestData.user(9, RoleName.HR_MANAGER), LocalDateTime.now());
        PayrollSummary summary = new PayrollSummary(run, TestData.dailyWage(1, "3600.00"),
                new AttendanceTotals(20, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("2")));

        PayslipComponent payslip = calculator.calculate(summary);

        // base 72,000; overtime 2h x 400 x 1.5 = 1,200; allowance 20 x 250 = 5,000; EPF 8% of 72,000 = 5,760
        assertThat(payslip.entries()).extracting(PayslipEntry::amount)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("72000"), new BigDecimal("1200"), new BigDecimal("5000"),
                        new BigDecimal("-5760"));
        assertThat(payslip.total()).isEqualByComparingTo("72440.00");
    }

    @Test
    void officeStaffWithoutOvertimeOrSiteDaysOnlyGetBaseAndEpf() {
        PayrollRun run = new PayrollRun(YearMonth.of(2026, 9), 26, TestData.user(9, RoleName.HR_MANAGER), LocalDateTime.now());
        PayrollSummary summary = new PayrollSummary(run, TestData.permanent(2, "160000.00"),
                new AttendanceTotals(0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

        PayslipComponent payslip = calculator.calculate(summary);

        assertThat(payslip.entries()).extracting(PayslipEntry::type)
                .containsExactly(PayslipEntry.LineType.BASE, PayslipEntry.LineType.DEDUCTION);
        assertThat(payslip.total()).isEqualByComparingTo("147200.00");
    }
}
