package com.sitemaster.dailyreport;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.material.Material;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * A material and quantity used on the day of a report. Deducted from site stock
 * when the PM approves the report.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable).
 */
@Entity
@Table(name = "daily_report_materials")
public class DailyReportMaterial extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, updatable = false)
    private DailyReport report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false, updatable = false)
    private Material material;

    @Column(name = "quantity", nullable = false, precision = 14, scale = 3, updatable = false)
    private BigDecimal quantity;

    protected DailyReportMaterial() {
        // required by JPA
    }

    public DailyReportMaterial(DailyReport report, Material material, BigDecimal quantity) {
        this.report = report;
        this.material = material;
        this.quantity = quantity;
    }

    public DailyReport getReport() {
        return report;
    }

    public Material getMaterial() {
        return material;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }
}
