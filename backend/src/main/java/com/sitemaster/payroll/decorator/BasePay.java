package com.sitemaster.payroll.decorator;

// PATTERN: Decorator

import java.math.BigDecimal;
import java.util.List;

/**
 * Decorator pattern - the CONCRETE COMPONENT: the base pay produced by the
 * salary strategy. All decorators wrap this.
 */
public class BasePay implements PayslipComponent {

    private final String label;
    private final BigDecimal amount;

    public BasePay(String label, BigDecimal amount) {
        this.label = label;
        this.amount = amount;
    }

    @Override
    public BigDecimal total() {
        return amount;
    }

    @Override
    public List<PayslipEntry> entries() {
        return List.of(new PayslipEntry(PayslipEntry.LineType.BASE, label, amount, amount));
    }

    @Override
    public BigDecimal basePay() {
        return amount;
    }
}
