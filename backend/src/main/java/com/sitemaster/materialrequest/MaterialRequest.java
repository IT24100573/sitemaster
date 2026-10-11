package com.sitemaster.materialrequest;

import com.sitemaster.common.entity.BaseRequest;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.Urgency;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.material.Material;
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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A site engineer's request for material, which flows through
 * PENDING -&gt; APPROVED -&gt; ORDERED -&gt; PAID -&gt; DELIVERED (or REJECTED).
 * <p>
 * OOP concepts:
 * <ul>
 *   <li>Inheritance - extends BaseRequest (status, requester, approver, remarks).</li>
 *   <li>Polymorphism - overrides {@link #approve(User, String)} to add the phase
 *       budget check: if the estimated cost exceeds the remaining phase budget the
 *       PM must explicitly acknowledge the overrun.</li>
 *   <li>Encapsulation - each later step (ordered, paid, delivered) has its own
 *       method that checks the current status first.</li>
 * </ul>
 */
@Entity
@Table(name = "material_requests")
public class MaterialRequest extends BaseRequest {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false, updatable = false)
    private Phase phase;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false, updatable = false)
    private Material material;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3, updatable = false)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false, length = 10, updatable = false)
    private Urgency urgency;

    @Column(name = "required_by", nullable = false, updatable = false)
    private LocalDate requiredBy;

    @Column(name = "estimated_cost", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal estimatedCost;

    @Column(name = "notes", length = 500, updatable = false)
    private String notes;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "delivered_quantity", precision = 14, scale = 3)
    private BigDecimal deliveredQuantity;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivered_by_id")
    private User deliveredBy;

    @Column(name = "delivery_notes", length = 500)
    private String deliveryNotes;

    /** Remaining phase budget, supplied by the service just before approval. */
    @Transient
    private BigDecimal remainingPhaseBudget;

    @Transient
    private boolean overrunAcknowledged;

    protected MaterialRequest() {
        // required by JPA
    }

    public MaterialRequest(Project project, Phase phase, Material material, BigDecimal quantity, Urgency urgency,
                           LocalDate requiredBy, String notes, User requestedBy) {
        super(requestedBy);
        this.project = project;
        this.phase = phase;
        this.material = material;
        this.quantity = quantity;
        this.urgency = urgency;
        this.requiredBy = requiredBy;
        this.notes = notes;
        this.estimatedCost = material.getUnitPrice().multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getRequestType() {
        return "Material request";
    }

    /** Must be called before {@link #approve} with the phase's remaining budget. */
    public void prepareBudgetCheck(BigDecimal remaining, boolean acknowledged) {
        this.remainingPhaseBudget = remaining;
        this.overrunAcknowledged = acknowledged;
    }

    public boolean exceeds(BigDecimal remaining) {
        return estimatedCost.compareTo(remaining) > 0;
    }

    @Override
    public void approve(User approver, String remarks) {
        if (remainingPhaseBudget == null) {
            throw new IllegalStateException("prepareBudgetCheck must be called before approve");
        }
        if (exceeds(remainingPhaseBudget) && !overrunAcknowledged) {
            throw BusinessRuleException.forField("acknowledgeOverrun", "materialRequest.budgetOverrun",
                    estimatedCost, remainingPhaseBudget);
        }
        super.approve(approver, remarks);
    }

    public void recordDecision(LocalDateTime now) {
        this.decidedAt = now;
    }

    /** A purchase order was raised. */
    public void markOrdered() {
        requireStatus(RequestStatus.APPROVED, "materialRequest.notApproved");
        transitionTo(RequestStatus.ORDERED);
    }

    /** The supplier declined the order: the accountant may order again elsewhere. */
    public void reopenForOrdering() {
        requireStatus(RequestStatus.ORDERED, "materialRequest.notOrdered");
        transitionTo(RequestStatus.APPROVED);
    }

    public void markPaid() {
        requireStatus(RequestStatus.ORDERED, "materialRequest.notOrdered");
        transitionTo(RequestStatus.PAID);
    }

    public void confirmDelivery(BigDecimal delivered, User receivedBy, String notes, LocalDateTime now) {
        requireStatus(RequestStatus.PAID, "materialRequest.notPaid");
        if (delivered == null || delivered.signum() <= 0 || delivered.compareTo(quantity) > 0) {
            throw BusinessRuleException.forField("deliveredQuantity", "materialRequest.delivered.range", quantity);
        }
        transitionTo(RequestStatus.DELIVERED);
        this.deliveredQuantity = delivered;
        this.deliveredBy = receivedBy;
        this.deliveryNotes = notes;
        this.deliveredAt = now;
    }

    private void requireStatus(RequestStatus expected, String messageKey) {
        if (getStatus() != expected) {
            throw new InvalidStatusTransitionException(messageKey);
        }
    }

    public Project getProject() {
        return project;
    }

    public Phase getPhase() {
        return phase;
    }

    public Material getMaterial() {
        return material;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public LocalDate getRequiredBy() {
        return requiredBy;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public BigDecimal getDeliveredQuantity() {
        return deliveredQuantity;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public User getDeliveredBy() {
        return deliveredBy;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }
}
