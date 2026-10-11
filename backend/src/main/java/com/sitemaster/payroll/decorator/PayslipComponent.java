package com.sitemaster.payroll.decorator;

// PATTERN: Decorator

import java.math.BigDecimal;
import java.util.List;

/**
 * Decorator pattern - the COMPONENT interface. A payslip is built by wrapping
 * {@link BasePay} in decorators (overtime, allowance, deduction); each layer adds
 * one step and the outermost layer knows the final net pay.
 * <p>
 * OOP concepts: Abstraction and Polymorphism.
 */
public interface PayslipComponent {

    /** Total after this component (and everything it wraps). */
    BigDecimal total();

    /** Every step from the base pay up to and including this component, in order. */
    List<PayslipEntry> entries();

    /** Base pay of the innermost component (deductions are calculated on it). */
    BigDecimal basePay();
}
