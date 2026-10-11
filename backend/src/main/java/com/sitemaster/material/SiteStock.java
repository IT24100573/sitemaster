package com.sitemaster.material;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * Quantity of one material held at one project site. Grows when deliveries are
 * confirmed and shrinks when approved daily reports record usage.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - the quantity
 * only changes through {@link #receive} and {@link #consume}, which never let it
 * go below zero.
 */
@Entity
@Table(name = "site_stock",
        uniqueConstraints = @UniqueConstraint(name = "uq_site_stock", columnNames = {"project_id", "material_id"}))
public class SiteStock extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false, updatable = false)
    private Material material;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3)
    private BigDecimal quantity = BigDecimal.ZERO;

    protected SiteStock() {
        // required by JPA
    }

    public SiteStock(Project project, Material material) {
        this.project = project;
        this.material = material;
    }

    public void receive(BigDecimal amount) {
        quantity = quantity.add(amount);
    }

    public void consume(BigDecimal amount) {
        if (amount.compareTo(quantity) > 0) {
            throw BusinessRuleException.forField("materialsUsed", "stock.insufficient", material.getName(), quantity);
        }
        quantity = quantity.subtract(amount);
    }

    public Project getProject() {
        return project;
    }

    public Material getMaterial() {
        return material;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
