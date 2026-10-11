package com.sitemaster.payroll.decorator;

// PATTERN: Decorator

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Decorator pattern - CONCRETE DECORATOR: adds the site allowance,
 * days on site x allowance per day (rate from System Settings).
 */
public class AllowanceDecorator extends PayslipDecorator {

    private final BigDecimal days;
    private final BigDecimal ratePerDay;

    public AllowanceDecorator(PayslipComponent inner, BigDecimal days, BigDecimal ratePerDay) {
        super(inner);
        this.days = days;
        this.ratePerDay = ratePerDay;
    }

    @Override
    protected BigDecimal amount() {
        return days.multiply(ratePerDay).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    protected String label() {
        return "Site allowance: " + days.stripTrailingZeros().toPlainString() + " day(s) x " + ratePerDay.toPlainString();
    }

    @Override
    protected PayslipEntry.LineType type() {
        return PayslipEntry.LineType.ALLOWANCE;
    }
}
