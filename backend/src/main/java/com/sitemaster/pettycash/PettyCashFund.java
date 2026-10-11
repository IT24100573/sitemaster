package com.sitemaster.pettycash;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.project.Project;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A project's petty cash float, held by a custodian on site.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - the balance
 * only moves through {@link #topUp}, {@link #spend} and {@link #adjustTo}, and
 * never goes below zero or above the float limit.
 */
@Entity
@Table(name = "petty_cash_funds")
public class PettyCashFund extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, unique = true, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "custodian_id", nullable = false)
    private User custodian;

    @Column(name = "float_limit", nullable = false, precision = 14, scale = 2)
    private BigDecimal floatLimit;

    @Column(name = "balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected PettyCashFund() {
        // required by JPA
    }

    public PettyCashFund(Project project) {
        this.project = project;
    }

    public void configure(User custodian, BigDecimal floatLimit, boolean active) {
        if (floatLimit.compareTo(balance) < 0) {
            throw BusinessRuleException.forField("floatLimit", "pettyCash.limit.belowBalance", balance);
        }
        this.custodian = custodian;
        this.floatLimit = floatLimit;
        this.active = active;
    }

    public void topUp(BigDecimal amount) {
        ensureActive();
        if (balance.add(amount).compareTo(floatLimit) > 0) {
            throw BusinessRuleException.forField("amount", "pettyCash.topUp.overLimit", floatLimit.subtract(balance));
        }
        balance = balance.add(amount);
    }

    public void spend(BigDecimal amount) {
        ensureActive();
        if (amount.compareTo(balance) > 0) {
            throw BusinessRuleException.forField("fromPettyCash", "pettyCash.insufficient", balance);
        }
        balance = balance.subtract(amount);
    }

    /** Sets the balance to the counted cash; returns the difference (signed). */
    public BigDecimal adjustTo(BigDecimal counted) {
        if (counted.signum() < 0 || counted.compareTo(floatLimit) > 0) {
            throw BusinessRuleException.forField("countedBalance", "pettyCash.count.range", floatLimit);
        }
        BigDecimal difference = counted.subtract(balance);
        balance = counted;
        return difference;
    }

    private void ensureActive() {
        if (!active) {
            throw new BusinessRuleException("pettyCash.inactive");
        }
    }

    public Project getProject() {
        return project;
    }

    public User getCustodian() {
        return custodian;
    }

    public BigDecimal getFloatLimit() {
        return floatLimit;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }
}
