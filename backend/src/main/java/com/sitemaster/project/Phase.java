package com.sitemaster.project;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.PhaseStatus;
import com.sitemaster.common.exception.BusinessRuleException;
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
 * A stage of a project (e.g. Foundations) with its own dates and budget.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (dates must
 * lie inside the project's dates; completion is guarded by {@link #complete}).
 */
@Entity
@Table(name = "phases")
public class Phase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "budget", nullable = false, precision = 14, scale = 2)
    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PhaseStatus status = PhaseStatus.NOT_STARTED;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected Phase() {
        // required by JPA
    }

    public Phase(Project project) {
        this.project = project;
    }

    public void update(String name, String description, LocalDate start, LocalDate end, BigDecimal budget, int sortOrder) {
        if (end.isBefore(start)) {
            throw BusinessRuleException.forField("endDate", "daterange.invalid");
        }
        if (!project.covers(start, end)) {
            throw BusinessRuleException.forField("startDate", "phase.dates.outsideProject",
                    project.getStartDate(), project.getEndDate());
        }
        this.name = name;
        this.description = description;
        this.startDate = start;
        this.endDate = end;
        this.budget = budget;
        this.sortOrder = sortOrder;
    }

    /** A phase can only be completed when it has no open tasks (checked by the caller). */
    public void complete(long openTasks) {
        if (openTasks > 0) {
            throw new BusinessRuleException("phase.complete.openTasks", openTasks);
        }
        status = PhaseStatus.COMPLETED;
    }

    public void start() {
        status = PhaseStatus.IN_PROGRESS;
    }

    /** Back to "not started"; only allowed while none of its tasks has started (checked by the caller). */
    public void reset() {
        status = PhaseStatus.NOT_STARTED;
    }

    public boolean covers(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public PhaseStatus getStatus() {
        return status;
    }

    public int getSortOrder() {
        return sortOrder;
    }
}
