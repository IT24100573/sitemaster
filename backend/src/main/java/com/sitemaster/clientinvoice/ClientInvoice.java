package com.sitemaster.clientinvoice;

import com.sitemaster.client.Client;
import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.project.Project;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An invoice to the project's client: DRAFT (editable) -&gt; ISSUED -&gt;
 * PARTIALLY_PAID -&gt; PAID. OVERDUE is shown, not stored: an issued invoice past
 * its due date that is not fully paid.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - totals are
 * always recalculated from the lines and the tax rate; only drafts can change;
 * receipts can never exceed what is owed.
 */
@Entity
@Table(name = "client_invoices")
public class ClientInvoice extends BaseEntity {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Column(name = "invoice_number", nullable = false, unique = true, length = 20, updatable = false)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, updatable = false)
    private Client client;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = BigDecimal.ZERO;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<ClientInvoiceLine> lines = new ArrayList<>();

    protected ClientInvoice() {
        // required by JPA
    }

    public ClientInvoice(String invoiceNumber, Project project) {
        this.invoiceNumber = invoiceNumber;
        this.project = project;
        this.client = project.getClient();
    }

    public void update(LocalDate issueDate, LocalDate dueDate, BigDecimal taxRate, String notes,
                       List<ClientInvoiceLine> newLines) {
        ensureDraft();
        if (dueDate.isBefore(issueDate)) {
            throw BusinessRuleException.forField("dueDate", "clientInvoice.due.beforeIssue");
        }
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.taxRate = taxRate;
        this.notes = notes;
        lines.clear();
        lines.addAll(newLines);
        subtotal = lines.stream().map(ClientInvoiceLine::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        taxAmount = subtotal.multiply(taxRate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        total = subtotal.add(taxAmount);
    }

    public void issue(LocalDateTime now) {
        ensureDraft();
        if (lines.isEmpty() || total.signum() <= 0) {
            throw new BusinessRuleException("clientInvoice.empty");
        }
        status = InvoiceStatus.ISSUED;
        issuedAt = now;
    }

    public BigDecimal outstanding() {
        return total.subtract(paidAmount);
    }

    public void receive(BigDecimal amount) {
        if (status == InvoiceStatus.DRAFT) {
            throw new InvalidStatusTransitionException("clientInvoice.notIssued");
        }
        if (status == InvoiceStatus.PAID) {
            throw new BusinessRuleException("clientInvoice.alreadyPaid");
        }
        if (amount.compareTo(outstanding()) > 0) {
            throw BusinessRuleException.forField("amount", "payment.amount.exceeds", outstanding());
        }
        paidAmount = paidAmount.add(amount);
        status = outstanding().signum() == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID;
    }

    /** Status shown to users, with OVERDUE derived from the due date. */
    public InvoiceStatus displayStatus(LocalDate today) {
        boolean open = status == InvoiceStatus.ISSUED || status == InvoiceStatus.PARTIALLY_PAID;
        return open && dueDate.isBefore(today) ? InvoiceStatus.OVERDUE : status;
    }

    public boolean isDraft() {
        return status == InvoiceStatus.DRAFT;
    }

    public void ensureDraft() {
        if (!isDraft()) {
            throw new InvalidStatusTransitionException("clientInvoice.notDraft");
        }
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public Project getProject() {
        return project;
    }

    public Client getClient() {
        return client;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public List<ClientInvoiceLine> getLines() {
        return Collections.unmodifiableList(lines);
    }
}
