package com.sitemaster.task;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.common.enums.Urgency;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.project.Phase;
import com.sitemaster.project.Project;
import com.sitemaster.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A unit of work in a phase, assigned to a project member.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - status and
 * progress always change together through {@link #applyProgress}: DONE means 100%,
 * 100% means DONE, TODO means 0%; the due date must stay inside the phase.
 */
@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phase_id", nullable = false)
    private Phase phase;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Urgency priority = Urgency.NORMAL;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "progress", nullable = false)
    private int progress;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<SubTask> subTasks = new ArrayList<>();

    protected Task() {
        // required by JPA
    }

    public Task(Project project) {
        this.project = project;
    }

    public void plan(Phase phase, String title, String description, Urgency priority, LocalDate dueDate) {
        if (!phase.covers(dueDate)) {
            throw BusinessRuleException.forField("dueDate", "task.dueDate.outsidePhase",
                    phase.getStartDate(), phase.getEndDate());
        }
        this.phase = phase;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
    }

    public void assignTo(User user) {
        this.assignee = user;
    }

    /** Keeps status and percentage consistent. */
    public void applyProgress(TaskStatus newStatus, int newProgress, LocalDateTime now) {
        if (newProgress < 0 || newProgress > 100) {
            throw BusinessRuleException.forField("progress", "percent.whole");
        }
        if (newStatus == TaskStatus.DONE || newProgress == 100) {
            status = TaskStatus.DONE;
            progress = 100;
            if (completedAt == null) {
                completedAt = now;
            }
            return;
        }
        completedAt = null;
        if (newStatus == TaskStatus.TODO && newProgress == 0) {
            status = TaskStatus.TODO;
            progress = 0;
        } else {
            status = TaskStatus.IN_PROGRESS;
            progress = newProgress;
        }
    }

    public boolean isOpen() {
        return status != TaskStatus.DONE;
    }

    public boolean isAssignedTo(Long userId) {
        return assignee != null && assignee.getId().equals(userId);
    }

    public void addSubTask(SubTask subTask) {
        subTasks.add(subTask);
    }

    public void removeSubTask(SubTask subTask) {
        subTasks.remove(subTask);
    }

    public Project getProject() {
        return project;
    }

    public Phase getPhase() {
        return phase;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public User getAssignee() {
        return assignee;
    }

    public Urgency getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public int getProgress() {
        return progress;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public List<SubTask> getSubTasks() {
        return Collections.unmodifiableList(subTasks);
    }
}
