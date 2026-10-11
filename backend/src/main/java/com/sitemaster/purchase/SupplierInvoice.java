package com.sitemaster.purchase;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.InvoiceStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.file.StoredFile;
import com.sitemaster.supplier.Supplier;
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
 * A supplier's invoice for one purchase order (amount never above the PO total).
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - payments are
 * recorded through {@link #recordPayment}, which keeps status and paid amount in step
 * and refuses overpayment.
 */
@Entity
@Table(name = "supplier_invoices")
public class SupplierInvoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false, unique = true, updatable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false, updatable = false)
    private Supplier supplier;

    @Column(name = "invoice_number", nullable = false, length = 50, updatable = false)
    private String invoiceNumber;

    @Column(name = "invoice_date", nullable = false, updatable = false)
    private LocalDate invoiceDate;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false, updatable = false)
    private StoredFile file;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    protected SupplierInvoice() {
        // required by JPA
    }

    public SupplierInvoice(PurchaseOrder purchaseOrder, String invoiceNumber, LocalDate invoiceDate, BigDecimal amount,
                           StoredFile file) {
        if (amount.compareTo(purchaseOrder.getTotalAmount()) > 0) {
            throw BusinessRuleException.forField("amount", "supplierInvoice.amount.exceedsPo", purchaseOrder.getTotalAmount());
        }
        this.purchaseOrder = purchaseOrder;
        this.supplier = purchaseOrder.getSupplier();
        this.invoiceNumber = invoiceNumber;
        this.invoiceDate = invoiceDate;
        this.amount = amount;
        this.file = file;
    }

    public BigDecimal outstanding() {
        return amount.subtract(paidAmount);
    }

    /** @return true when the invoice is now fully paid */
    public boolean recordPayment(BigDecimal payment) {
        if (status == InvoiceStatus.PAID) {
            throw new BusinessRuleException("supplierInvoice.alreadyPaid");
        }
        if (payment.compareTo(outstanding()) > 0) {
            throw BusinessRuleException.forField("amount", "payment.amount.exceeds", outstanding());
        }
        paidAmount = paidAmount.add(payment);
        status = outstanding().signum() == 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID;
        return status == InvoiceStatus.PAID;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public StoredFile getFile() {
        return file;
    }

    public InvoiceStatus getStatus() {
        return status;
    }
}
