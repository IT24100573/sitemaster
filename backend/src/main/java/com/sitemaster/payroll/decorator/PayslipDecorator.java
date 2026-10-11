package com.sitemaster.payroll.decorator;

// PATTERN: Decorator

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Decorator pattern - the abstract DECORATOR. It holds the wrapped component,
 * forwards to it, and adds exactly one step defined by the subclass.
 * <p>
 * OOP concepts: Inheritance (concrete decorators extend it), Polymorphism
 * (it is itself a PayslipComponent) and Abstraction (the step is abstract).
 */
public abstract class PayslipDecorator implements PayslipComponent {

    protected final PayslipComponent inner;

    protected PayslipDecorator(PayslipComponent inner) {
        this.inner = inner;
    }

    /** Signed amount this layer adds (negative for deductions). */
    protected abstract BigDecimal amount();

    protected abstract String label();

    protected abstract PayslipEntry.LineType type();

    @Override
    public BigDecimal total() {
        return inner.total().add(amount()).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<PayslipEntry> entries() {
        List<PayslipEntry> entries = new ArrayList<>(inner.entries());
        entries.add(new PayslipEntry(type(), label(), amount().setScale(2, RoundingMode.HALF_UP), total()));
        return entries;
    }

    @Override
    public BigDecimal basePay() {
        return inner.basePay();
    }
}
