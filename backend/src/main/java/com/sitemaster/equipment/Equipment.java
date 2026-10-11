package com.sitemaster.equipment;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.EquipmentStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.project.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Locale;

/**
 * A machine or tool (excavator, mixer...) that can be assigned to one site.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - assignment
 * keeps status consistent (assigned = IN_USE, released = AVAILABLE) and retired
 * equipment cannot be assigned.
 */
@Entity
@Table(name = "equipment")
public class Equipment extends BaseEntity {

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EquipmentStatus status = EquipmentStatus.AVAILABLE;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "last_service_date")
    private LocalDate lastServiceDate;

    @Column(name = "next_service_date")
    private LocalDate nextServiceDate;

    protected Equipment() {
        // required by JPA
    }

    public Equipment(String code) {
        this.code = code.trim().toUpperCase(Locale.ROOT);
    }

    public void update(String code, String name, String category) {
        this.code = code.trim().toUpperCase(Locale.ROOT);
        this.name = name;
        this.category = category;
    }

    public void assignTo(Project target) {
        if (status == EquipmentStatus.RETIRED) {
            throw BusinessRuleException.forField("projectId", "equipment.retired");
        }
        this.project = target;
        if (target == null) {
            if (status == EquipmentStatus.IN_USE) {
                status = EquipmentStatus.AVAILABLE;
            }
        } else if (status == EquipmentStatus.AVAILABLE) {
            status = EquipmentStatus.IN_USE;
        }
    }

    public void updateCondition(EquipmentStatus newStatus, String notes, LocalDate lastService, LocalDate nextService) {
        if (nextService != null && lastService != null && nextService.isBefore(lastService)) {
            throw BusinessRuleException.forField("nextServiceDate", "daterange.invalid");
        }
        if (newStatus == EquipmentStatus.IN_USE && project == null) {
            throw BusinessRuleException.forField("status", "equipment.inUse.unassigned");
        }
        if (newStatus == EquipmentStatus.RETIRED) {
            project = null;
        }
        this.status = newStatus;
        this.notes = notes;
        this.lastServiceDate = lastService;
        this.nextServiceDate = nextService;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Project getProject() {
        return project;
    }

    public EquipmentStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDate getLastServiceDate() {
        return lastServiceDate;
    }

    public LocalDate getNextServiceDate() {
        return nextServiceDate;
    }
}
