package com.sitemaster.payroll.decorator;

// PATTERN: Decorator

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Decorator pattern - CONCRETE DECORATOR: subtracts a percentage of the base pay
 * (e.g. EPF employee contribution; rate from System Settings).
 */
public class DeductionDecorator extends PayslipDecorator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final String name;
    private final BigDecimal ratePercent;

    public DeductionDecorator(PayslipComponent inner, String name, BigDecimal ratePercent) {
        super(inner);
        this.name = name;
        this.ratePercent = ratePercent;
    }

    @Override
    protected BigDecimal amount() {
        return basePay().multiply(ratePercent).divide(HUNDRED, 2, RoundingMode.HALF_UP).negate();
    }

    @Override
    protected String label() {
        return name + ": " + ratePercent.stripTrailingZeros().toPlainString() + "% of base pay";
    }

    @Override
    protected PayslipEntry.LineType type() {
        return PayslipEntry.LineType.DEDUCTION;
    }
}
