package com.sitemaster.task;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A piece of a task that a site engineer gives to a worker.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (the
 * completion time follows the status automatically).
 */
@Entity
@Table(name = "sub_tasks")
public class SubTask extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false, updatable = false)
    private Task task;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status = TaskStatus.TODO;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected SubTask() {
        // required by JPA
    }

    public SubTask(Task task) {
        this.task = task;
    }

    public void update(String title, User assignee, LocalDate dueDate) {
        if (dueDate != null && !task.getPhase().covers(dueDate)) {
            throw BusinessRuleException.forField("dueDate", "task.dueDate.outsidePhase",
                    task.getPhase().getStartDate(), task.getPhase().getEndDate());
        }
        this.title = title;
        this.assignee = assignee;
        this.dueDate = dueDate;
    }

    public void changeStatus(TaskStatus newStatus, LocalDateTime now) {
        this.status = newStatus;
        this.completedAt = newStatus == TaskStatus.DONE ? now : null;
    }

    public boolean isAssignedTo(Long userId) {
        return assignee != null && assignee.getId().equals(userId);
    }

    public Task getTask() {
        return task;
    }

    public String getTitle() {
        return title;
    }

    public User getAssignee() {
        return assignee;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
