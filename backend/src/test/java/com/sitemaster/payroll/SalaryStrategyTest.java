package com.sitemaster.payroll;

import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.payroll.strategy.ContractSalaryStrategy;
import com.sitemaster.payroll.strategy.DailyWageSalaryStrategy;
import com.sitemaster.payroll.strategy.PermanentSalaryStrategy;
import com.sitemaster.payroll.strategy.SalaryCalculationStrategy;
import com.sitemaster.payroll.strategy.SalaryContext;
import com.sitemaster.payroll.strategy.SalaryStrategyResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static com.sitemaster.support.TestData.contract;
import static com.sitemaster.support.TestData.dailyWage;
import static com.sitemaster.support.TestData.permanent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Strategy pattern: each employment type works out base pay its own way. */
class SalaryStrategyTest {

    private static final YearMonth MONTH = YearMonth.of(2026, 9);
    private static final BigDecimal NINE_HOURS = new BigDecimal("9.00");

    private static AttendanceTotals totals(int present, int half, int absent, String paidLeave, String unpaidLeave) {
        return new AttendanceTotals(present, half, absent, new BigDecimal(paidLeave), new BigDecimal(unpaidLeave),
                BigDecimal.ZERO);
    }

    @Nested
    @DisplayName("Daily wage")
    class DailyWage {

        private final SalaryCalculationStrategy strategy = new DailyWageSalaryStrategy();

        @Test
        void paysDaysWorkedWithHalfDaysAtHalfRate() {
            SalaryContext ctx = new SalaryContext(dailyWage(1, "3500.00"), MONTH, totals(20, 2, 1, "0", "0"), 25, NINE_HOURS);
            assertThat(strategy.basePay(ctx)).isEqualByComparingTo("73500.00");
        }

        @Test
        void hourlyRateIsDailyRateOverWorkingHours() {
            SalaryContext ctx = new SalaryContext(dailyWage(1, "3600.00"), MONTH, totals(0, 0, 0, "0", "0"), 25, NINE_HOURS);
            assertThat(strategy.hourlyRate(ctx)).isEqualByComparingTo("400.00");
        }

        @Test
        void leaveIsNotPaidToDailyWageWorkers() {
            SalaryContext ctx = new SalaryContext(dailyWage(1, "3000.00"), MONTH, totals(10, 0, 0, "5", "0"), 25, NINE_HOURS);
            assertThat(strategy.basePay(ctx)).isEqualByComparingTo("30000.00");
        }
    }

    @Nested
    @DisplayName("Permanent")
    class Permanent {

        private final SalaryCalculationStrategy strategy = new PermanentSalaryStrategy();

        @Test
        void paysFullSalaryWhenNothingUnpaidIsRecorded() {
            SalaryContext ctx = new SalaryContext(permanent(2, "150000.00"), MONTH, totals(0, 0, 0, "0", "0"), 25, NINE_HOURS);
            assertThat(strategy.basePay(ctx)).isEqualByComparingTo("150000.00");
        }

        @Test
        void deductsAbsenceUnpaidLeaveAndHalfOfHalfDays() {
            // unpaid = 2 absent + 1 unpaid leave + 0.5 = 3.5 of 25 days
            SalaryContext ctx = new SalaryContext(permanent(2, "100000.00"), MONTH, totals(20, 1, 2, "2", "1"), 25, NINE_HOURS);
            assertThat(strategy.basePay(ctx)).isEqualByComparingTo("86000.00");
        }

        @Test
        void neverPaysLessThanZero() {
            SalaryContext ctx = new SalaryContext(permanent(2, "100000.00"), MONTH, totals(0, 0, 30, "0", "0"), 25, NINE_HOURS);
            assertThat(strategy.basePay(ctx)).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Contract")
    class Contract {

        private final ContractSalaryStrategy strategy = new ContractSalaryStrategy();

        @Test
        void spreadsContractAmountOverItsMonths() {
            SalaryContext ctx = new SalaryContext(contract(3, "600000.00", LocalDate.of(2026, 7, 15),
                    LocalDate.of(2026, 12, 10)), MONTH, totals(0, 0, 0, "0", "0"), 25, NINE_HOURS);
            assertThat(strategy.basePay(ctx)).isEqualByComparingTo("100000.00");
        }

        @Test
        void paysNothingOutsideTheContract() {
            SalaryContext ctx = new SalaryContext(contract(3, "600000.00", LocalDate.of(2026, 10, 1),
                    LocalDate.of(2027, 3, 31)), MONTH, totals(0, 0, 0, "0", "0"), 25, NINE_HOURS);
            assertThat(strategy.basePay(ctx)).isEqualByComparingTo("0.00");
        }
    }

    @Test
    void resolverPicksTheStrategyForEachType() {
        SalaryStrategyResolver resolver = new SalaryStrategyResolver(List.of(new DailyWageSalaryStrategy(),
                new PermanentSalaryStrategy(), new ContractSalaryStrategy()));
        assertThat(resolver.forType(EmploymentType.DAILY_WAGE)).isInstanceOf(DailyWageSalaryStrategy.class);
        assertThat(resolver.forType(EmploymentType.PERMANENT)).isInstanceOf(PermanentSalaryStrategy.class);
        assertThat(resolver.forType(EmploymentType.CONTRACT)).isInstanceOf(ContractSalaryStrategy.class);
    }

    @Test
    void resolverFailsLoudlyForAMissingStrategy() {
        SalaryStrategyResolver resolver = new SalaryStrategyResolver(List.of(new DailyWageSalaryStrategy()));
        assertThatThrownBy(() -> resolver.forType(EmploymentType.PERMANENT)).isInstanceOf(IllegalStateException.class);
    }
}
