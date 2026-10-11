package com.sitemaster.project;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A key date in a project (e.g. "Roof slab cast").
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (completion
 * state and date always change together).
 */
@Entity
@Table(name = "milestones")
public class Milestone extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    private Phase phase;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    protected Milestone() {
        // required by JPA
    }

    public Milestone(Project project) {
        this.project = project;
    }

    public void update(String name, LocalDate dueDate, Phase phase) {
        if (!project.covers(dueDate, dueDate)) {
            throw BusinessRuleException.forField("dueDate", "milestone.date.outsideProject",
                    project.getStartDate(), project.getEndDate());
        }
        this.name = name;
        this.dueDate = dueDate;
        this.phase = phase;
    }

    public void markCompleted(boolean done, LocalDate today) {
        this.completed = done;
        this.completedDate = done ? today : null;
    }

    public Project getProject() {
        return project;
    }

    public Phase getPhase() {
        return phase;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public boolean isCompleted() {
        return completed;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }
}
