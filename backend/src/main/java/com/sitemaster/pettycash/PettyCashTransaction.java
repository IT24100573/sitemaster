package com.sitemaster.pettycash;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.expense.ExpenseRequest;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One movement of a petty cash fund (signed amount) with the balance after it.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable).
 */
@Entity
@Table(name = "petty_cash_transactions")
public class PettyCashTransaction extends BaseEntity {

    /** Kinds of movement. */
    public enum Type {
        TOP_UP,
        EXPENSE,
        ADJUSTMENT
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fund_id", nullable = false, updatable = false)
    private PettyCashFund fund;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20, updatable = false)
    private Type type;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal balanceAfter;

    @Column(name = "transaction_date", nullable = false, updatable = false)
    private LocalDate transactionDate;

    @Column(name = "description", nullable = false, length = 255, updatable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_request_id", updatable = false)
    private ExpenseRequest expenseRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_id", nullable = false, updatable = false)
    private User recordedBy;

    protected PettyCashTransaction() {
        // required by JPA
    }

    public PettyCashTransaction(PettyCashFund fund, Type type, BigDecimal amount, LocalDate date, String description,
                                ExpenseRequest expense, User recordedBy) {
        this.fund = fund;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = fund.getBalance();
        this.transactionDate = date;
        this.description = description;
        this.expenseRequest = expense;
        this.recordedBy = recordedBy;
    }

    public PettyCashFund getFund() {
        return fund;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public ExpenseRequest getExpenseRequest() {
        return expenseRequest;
    }

    public User getRecordedBy() {
        return recordedBy;
    }
}
