package com.sitemaster.purchase;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.material.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One line of a purchase order; the line total is calculated, never entered.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable).
 */
@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false, updatable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false, updatable = false)
    private Material material;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3, updatable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal lineTotal;

    protected PurchaseOrderItem() {
        // required by JPA
    }

    public PurchaseOrderItem(PurchaseOrder purchaseOrder, Material material, BigDecimal quantity, BigDecimal unitPrice) {
        this.purchaseOrder = purchaseOrder;
        this.material = material;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = unitPrice.multiply(quantity).setScale(2, RoundingMode.HALF_UP);
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public Material getMaterial() {
        return material;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }
}
