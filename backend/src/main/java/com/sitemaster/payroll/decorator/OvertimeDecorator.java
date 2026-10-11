package com.sitemaster.payroll.decorator;

// PATTERN: Decorator

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Decorator pattern - CONCRETE DECORATOR: adds approved overtime at
 * hours x hourly rate x overtime multiplier (multiplier from System Settings).
 */
public class OvertimeDecorator extends PayslipDecorator {

    private final BigDecimal hours;
    private final BigDecimal hourlyRate;
    private final BigDecimal multiplier;

    public OvertimeDecorator(PayslipComponent inner, BigDecimal hours, BigDecimal hourlyRate, BigDecimal multiplier) {
        super(inner);
        this.hours = hours;
        this.hourlyRate = hourlyRate;
        this.multiplier = multiplier;
    }

    @Override
    protected BigDecimal amount() {
        return hours.multiply(hourlyRate).multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    protected String label() {
        return "Overtime: " + hours.stripTrailingZeros().toPlainString() + " h x " + hourlyRate.toPlainString()
                + " x " + multiplier.stripTrailingZeros().toPlainString();
    }

    @Override
    protected PayslipEntry.LineType type() {
        return PayslipEntry.LineType.OVERTIME;
    }
}
