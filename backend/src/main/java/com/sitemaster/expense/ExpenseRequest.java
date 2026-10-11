package com.sitemaster.expense;

import com.sitemaster.common.entity.BaseRequest;
import com.sitemaster.common.enums.ExpenseCategory;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.file.StoredFile;
import com.sitemaster.project.Phase;
import com.sitemaster.project.Project;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * A site cost (labour hire, fuel, small tools...) to be approved by the
 * project's PM and paid by the Accountant: PENDING -&gt; APPROVED -&gt; PAID (or REJECTED).
 * <p>
 * OOP concepts:
 * <ul>
 *   <li>Inheritance - extends BaseRequest.</li>
 *   <li>Polymorphism - {@link #allowedStatuses()} narrows the status set and
 *       {@link #approve(User, String)} adds the phase budget check (an overrun must
 *       be acknowledged), like MaterialRequest.</li>
 *   <li>Encapsulation - a receipt is required above the System Settings threshold
 *       ({@link #requireReceipt}); only APPROVED expenses can be paid.</li>
 * </ul>
 */
@Entity
@Table(name = "expense_requests")
public class ExpenseRequest extends BaseRequest {

    private static final Set<RequestStatus> EXPENSE_STATUSES =
            EnumSet.of(RequestStatus.PENDING, RequestStatus.APPROVED, RequestStatus.REJECTED, RequestStatus.PAID);

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false, updatable = false)
    private Phase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20, updatable = false)
    private ExpenseCategory category;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "expense_date", nullable = false, updatable = false)
    private LocalDate expenseDate;

    @Column(name = "payee", nullable = false, length = 100, updatable = false)
    private String payee;

    @Column(name = "description", nullable = false, length = 500, updatable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receipt_file_id")
    private StoredFile receipt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Transient
    private BigDecimal remainingPhaseBudget;

    @Transient
    private boolean overrunAcknowledged;

    protected ExpenseRequest() {
        // required by JPA
    }

    public ExpenseRequest(Project project, Phase phase, ExpenseCategory category, BigDecimal amount,
                          LocalDate expenseDate, String payee, String description, User requestedBy) {
        super(requestedBy);
        this.project = project;
        this.phase = phase;
        this.category = category;
        this.amount = amount;
        this.expenseDate = expenseDate;
        this.payee = payee;
        this.description = description;
    }

    @Override
    public String getRequestType() {
        return "Expense request";
    }

    @Override
    protected Set<RequestStatus> allowedStatuses() {
        return EXPENSE_STATUSES;
    }

    /** Amounts above the threshold must come with a receipt. */
    public void requireReceipt(BigDecimal threshold) {
        if (threshold != null && amount.compareTo(threshold) > 0 && receipt == null) {
            throw BusinessRuleException.forField("receipt", "expense.receipt.required", threshold);
        }
    }

    public void attachReceipt(StoredFile file) {
        this.receipt = file;
    }

    public void prepareBudgetCheck(BigDecimal remaining, boolean acknowledged) {
        this.remainingPhaseBudget = remaining;
        this.overrunAcknowledged = acknowledged;
    }

    @Override
    public void approve(User approver, String remarks) {
        if (remainingPhaseBudget == null) {
            throw new IllegalStateException("prepareBudgetCheck must be called before approve");
        }
        if (amount.compareTo(remainingPhaseBudget) > 0 && !overrunAcknowledged) {
            throw BusinessRuleException.forField("acknowledgeOverrun", "expense.budgetOverrun", amount,
                    remainingPhaseBudget);
        }
        super.approve(approver, remarks);
    }

    public void recordDecision(LocalDateTime now) {
        this.decidedAt = now;
    }

    public void markPaid(LocalDateTime now) {
        if (getStatus() != RequestStatus.APPROVED) {
            throw new InvalidStatusTransitionException("expense.notApproved");
        }
        transitionTo(RequestStatus.PAID);
        this.paidAt = now;
    }

    public Project getProject() {
        return project;
    }

    public Phase getPhase() {
        return phase;
    }

    public ExpenseCategory getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getExpenseDate() {
        return expenseDate;
    }

    public String getPayee() {
        return payee;
    }

    public String getDescription() {
        return description;
    }

    public StoredFile getReceipt() {
        return receipt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
