package com.sitemaster.payroll.strategy;

// PATTERN: Strategy

import com.sitemaster.common.enums.EmploymentType;

import java.math.BigDecimal;

/**
 * Strategy pattern - the STRATEGY interface. Each employment type pays a
 * different way; the payroll service picks the right implementation at runtime
 * through {@link SalaryStrategyResolver} and never uses if/else on the type.
 * <p>
 * Implementations: DailyWageSalaryStrategy, PermanentSalaryStrategy,
 * ContractSalaryStrategy.
 * <p>
 * OOP concepts: Abstraction and Polymorphism.
 */
public interface SalaryCalculationStrategy {

    /** The employment type this strategy pays. */
    EmploymentType supports();

    /** Base pay for the month, before overtime, allowances and deductions. */
    BigDecimal basePay(SalaryContext context);

    /** Normal hourly rate, used to price overtime. */
    BigDecimal hourlyRate(SalaryContext context);

    /** Short explanation of the base pay, shown on the payslip line. */
    String describe(SalaryContext context);
}
