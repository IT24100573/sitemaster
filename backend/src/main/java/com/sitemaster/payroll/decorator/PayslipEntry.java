package com.sitemaster.payroll.decorator;

import java.math.BigDecimal;

/**
 * One step of a payslip calculation.
 *
 * @param type         BASE, OVERTIME, ALLOWANCE or DEDUCTION
 * @param label        text shown on the payslip
 * @param amount       signed amount (deductions are negative)
 * @param runningTotal total after this step
 */
public record PayslipEntry(LineType type, String label, BigDecimal amount, BigDecimal runningTotal) {

    /** Kinds of payslip line. */
    public enum LineType {
        BASE,
        OVERTIME,
        ALLOWANCE,
        DEDUCTION
    }
}
