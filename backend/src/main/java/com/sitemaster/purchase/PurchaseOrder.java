package com.sitemaster.purchase;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.PurchaseOrderStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.materialrequest.MaterialRequest;
import com.sitemaster.project.Project;
import com.sitemaster.supplier.Supplier;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * An order sent to a supplier for an approved material request.
 * SENT -&gt; ACCEPTED (or DECLINED) -&gt; DISPATCHED -&gt; INVOICED -&gt; PAID -&gt; DELIVERED.
 * The supplier may also invoice before dispatching; dispatch is then recorded
 * without changing the INVOICED status.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - every step is
 * a method that checks the current status, and the total is derived from the items.
 */
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends BaseEntity {

    public static final int MIN_REASON_LENGTH = 5;

    @Column(name = "po_number", nullable = false, unique = true, length = 20, updatable = false)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_request_id", nullable = false, updatable = false)
    private MaterialRequest materialRequest;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false, updatable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseOrderStatus status = PurchaseOrderStatus.SENT;

    @Column(name = "order_date", nullable = false, updatable = false)
    private LocalDate orderDate;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "notes", length = 500, updatable = false)
    private String notes;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "decline_reason", length = 500)
    private String declineReason;

    @Column(name = "dispatched_at")
    private LocalDateTime dispatchedAt;

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    protected PurchaseOrder() {
        // required by JPA
    }

    public PurchaseOrder(String poNumber, MaterialRequest request, Supplier supplier, LocalDate orderDate, String notes) {
        this.poNumber = poNumber;
        this.materialRequest = request;
        this.supplier = supplier;
        this.project = request.getProject();
        this.orderDate = orderDate;
        this.notes = notes;
    }

    public void addItem(PurchaseOrderItem item) {
        items.add(item);
        totalAmount = items.stream().map(PurchaseOrderItem::getLineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void accept(LocalDateTime now) {
        require(EnumSet.of(PurchaseOrderStatus.SENT));
        status = PurchaseOrderStatus.ACCEPTED;
        respondedAt = now;
    }

    public void decline(String reason, LocalDateTime now) {
        require(EnumSet.of(PurchaseOrderStatus.SENT));
        if (reason == null || reason.trim().length() < MIN_REASON_LENGTH) {
            throw BusinessRuleException.forField("reason", "purchaseOrder.decline.reasonRequired", MIN_REASON_LENGTH);
        }
        status = PurchaseOrderStatus.DECLINED;
        declineReason = reason.trim();
        respondedAt = now;
    }

    public void dispatch(LocalDate expectedDelivery, LocalDate today, LocalDateTime now) {
        boolean invoicedNotDispatched = status == PurchaseOrderStatus.INVOICED && dispatchedAt == null;
        if (status != PurchaseOrderStatus.ACCEPTED && !invoicedNotDispatched) {
            throw new InvalidStatusTransitionException("purchaseOrder.dispatch.notAllowed");
        }
        if (expectedDelivery.isBefore(today)) {
            throw BusinessRuleException.forField("expectedDeliveryDate", "purchaseOrder.expected.past");
        }
        dispatchedAt = now;
        expectedDeliveryDate = expectedDelivery;
        if (status == PurchaseOrderStatus.ACCEPTED) {
            status = PurchaseOrderStatus.DISPATCHED;
        }
    }

    public void markInvoiced() {
        if (status != PurchaseOrderStatus.ACCEPTED && status != PurchaseOrderStatus.DISPATCHED) {
            throw new InvalidStatusTransitionException("supplierInvoice.poStatus");
        }
        status = PurchaseOrderStatus.INVOICED;
    }

    public void markPaid(LocalDateTime now) {
        require(EnumSet.of(PurchaseOrderStatus.INVOICED));
        status = PurchaseOrderStatus.PAID;
        paidAt = now;
    }

    public void markDelivered(LocalDateTime now) {
        require(EnumSet.of(PurchaseOrderStatus.PAID));
        status = PurchaseOrderStatus.DELIVERED;
        deliveredAt = now;
    }

    public boolean belongsTo(Long supplierId) {
        return supplier.getId().equals(supplierId);
    }

    private void require(EnumSet<PurchaseOrderStatus> allowed) {
        if (!allowed.contains(status)) {
            throw new InvalidStatusTransitionException("purchaseOrder.status.transition", status);
        }
    }

    public String getPoNumber() {
        return poNumber;
    }

    public MaterialRequest getMaterialRequest() {
        return materialRequest;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public Project getProject() {
        return project;
    }

    public PurchaseOrderStatus getStatus() {
        return status;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public List<PurchaseOrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
