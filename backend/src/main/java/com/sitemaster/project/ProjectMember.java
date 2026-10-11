package com.sitemaster.project;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Assigns a user to a project. PMs, site engineers and workers only see the
 * projects they are members of.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable pair).
 */
@Entity
@Table(name = "project_members",
        uniqueConstraints = @UniqueConstraint(name = "uq_project_members", columnNames = {"project_id", "user_id"}))
public class ProjectMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    protected ProjectMember() {
        // required by JPA
    }

    public ProjectMember(Project project, User user) {
        this.project = project;
        this.user = user;
    }

    public Project getProject() {
        return project;
    }

    public User getUser() {
        return user;
    }
}
