package com.sitemaster.payroll;

import com.sitemaster.payroll.decorator.AllowanceDecorator;
import com.sitemaster.payroll.decorator.BasePay;
import com.sitemaster.payroll.decorator.DeductionDecorator;
import com.sitemaster.payroll.decorator.OvertimeDecorator;
import com.sitemaster.payroll.decorator.PayslipComponent;
import com.sitemaster.payroll.decorator.PayslipEntry;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Decorator pattern: each layer adds exactly one payslip line with a running total. */
class PayslipDecoratorTest {

    @Test
    void basePayAloneIsOneLine() {
        PayslipComponent payslip = new BasePay("Base", new BigDecimal("50000.00"));
        assertThat(payslip.total()).isEqualByComparingTo("50000.00");
        assertThat(payslip.entries()).hasSize(1);
    }

    @Test
    void layersAddUpInOrderWithRunningTotals() {
        PayslipComponent payslip = new BasePay("Base", new BigDecimal("70000.00"));
        payslip = new OvertimeDecorator(payslip, new BigDecimal("4"), new BigDecimal("400.00"), new BigDecimal("1.5"));
        payslip = new AllowanceDecorator(payslip, new BigDecimal("20"), new BigDecimal("250.00"));
        payslip = new DeductionDecorator(payslip, "EPF", new BigDecimal("8"));

        List<PayslipEntry> entries = payslip.entries();
        assertThat(entries).extracting(PayslipEntry::type).containsExactly(PayslipEntry.LineType.BASE,
                PayslipEntry.LineType.OVERTIME, PayslipEntry.LineType.ALLOWANCE, PayslipEntry.LineType.DEDUCTION);
        assertThat(entries.get(1).amount()).isEqualByComparingTo("2400.00");   // 4 x 400 x 1.5
        assertThat(entries.get(2).amount()).isEqualByComparingTo("5000.00");   // 20 x 250
        assertThat(entries.get(3).amount()).isEqualByComparingTo("-5600.00");  // 8% of base 70,000
        assertThat(entries.get(2).runningTotal()).isEqualByComparingTo("77400.00");
        assertThat(payslip.total()).isEqualByComparingTo("71800.00");
    }

    @Test
    void deductionIsOnBasePayNotOnOvertime() {
        PayslipComponent payslip = new OvertimeDecorator(new BasePay("Base", new BigDecimal("10000.00")),
                new BigDecimal("10"), new BigDecimal("100.00"), new BigDecimal("2"));
        payslip = new DeductionDecorator(payslip, "EPF", new BigDecimal("10"));
        assertThat(payslip.entries().get(2).amount()).isEqualByComparingTo("-1000.00");
        assertThat(payslip.basePay()).isEqualByComparingTo("10000.00");
    }
}
