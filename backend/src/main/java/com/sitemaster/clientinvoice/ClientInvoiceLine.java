package com.sitemaster.clientinvoice;

import com.sitemaster.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * One billed item; the line total is calculated, never entered.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable).
 */
@Entity
@Table(name = "client_invoice_lines")
public class ClientInvoiceLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, updatable = false)
    private ClientInvoice invoice;

    @Column(name = "line_order", nullable = false, updatable = false)
    private int lineOrder;

    @Column(name = "description", nullable = false, length = 255, updatable = false)
    private String description;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3, updatable = false)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2, updatable = false)
    private BigDecimal lineTotal;

    protected ClientInvoiceLine() {
        // required by JPA
    }

    public ClientInvoiceLine(ClientInvoice invoice, int lineOrder, String description, BigDecimal quantity,
                             BigDecimal unitPrice) {
        this.invoice = invoice;
        this.lineOrder = lineOrder;
        this.description = description;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public ClientInvoice getInvoice() {
        return invoice;
    }

    public int getLineOrder() {
        return lineOrder;
    }

    public String getDescription() {
        return description;
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
