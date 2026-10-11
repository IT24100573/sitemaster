package com.sitemaster.material;

import com.sitemaster.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A catalogue item (e.g. "Cement OPC 50kg") with its unit and standard price,
 * used to estimate the cost of material requests.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (soft delete
 * through activate/deactivate).
 */
@Entity
@Table(name = "materials")
public class Material extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Material() {
        // required by JPA
    }

    public Material(String name) {
        this.name = name;
    }

    public void update(String name, String category, String unit, BigDecimal unitPrice, String description) {
        this.name = name;
        this.category = category;
        this.unit = unit;
        this.unitPrice = unitPrice;
        this.description = description;
    }

    public void activate() {
        active = true;
    }

    public void deactivate() {
        active = false;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getUnit() {
        return unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }
}
