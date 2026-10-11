package com.sitemaster.project;

import com.sitemaster.client.Client;
import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.ProjectStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.file.StoredFile;
import com.sitemaster.user.User;
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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * A construction project.
 * <p>
 * OOP concepts:
 * <ul>
 *   <li>Inheritance - extends BaseEntity.</li>
 *   <li>Encapsulation - the status can only change through {@link #changeStatus},
 *       which enforces the allowed transitions (PLANNED-&gt;ACTIVE, ACTIVE&lt;-&gt;DELAYED,
 *       ACTIVE/DELAYED-&gt;COMPLETED, COMPLETED-&gt;ARCHIVED). Archiving is the soft delete
 *       and clears the active flag. Dates are set together so end &gt; start always holds.</li>
 * </ul>
 */
@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    private static final Map<ProjectStatus, Set<ProjectStatus>> TRANSITIONS = new EnumMap<>(ProjectStatus.class);

    static {
        TRANSITIONS.put(ProjectStatus.PLANNED, EnumSet.of(ProjectStatus.ACTIVE));
        TRANSITIONS.put(ProjectStatus.ACTIVE, EnumSet.of(ProjectStatus.DELAYED, ProjectStatus.COMPLETED));
        TRANSITIONS.put(ProjectStatus.DELAYED, EnumSet.of(ProjectStatus.ACTIVE, ProjectStatus.COMPLETED));
        TRANSITIONS.put(ProjectStatus.COMPLETED, EnumSet.of(ProjectStatus.ARCHIVED));
        TRANSITIONS.put(ProjectStatus.ARCHIVED, EnumSet.noneOf(ProjectStatus.class));
    }

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "latitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "budget", nullable = false, precision = 14, scale = 2)
    private BigDecimal budget;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProjectStatus status = ProjectStatus.PLANNED;

    @Column(name = "progress", nullable = false)
    private int progress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cover_photo_id")
    private StoredFile coverPhoto;

    @Column(name = "completed_date")
    private LocalDate completedDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    protected Project() {
        // required by JPA
    }

    public Project(String name, Client client, User manager) {
        this.name = name;
        this.client = client;
        this.manager = manager;
    }

    /** Applies the transition rules; COMPLETED records the completion date, ARCHIVED soft-deletes. */
    public void changeStatus(ProjectStatus target, LocalDate today) {
        if (target == status) {
            return;
        }
        if (!TRANSITIONS.get(status).contains(target)) {
            throw InvalidStatusTransitionException.forField("status", "project.status.transition", status, target);
        }
        if (target == ProjectStatus.COMPLETED) {
            completedDate = today;
            progress = 100;
        }
        if (target == ProjectStatus.ARCHIVED) {
            active = false;
        }
        status = target;
    }

    /** Statuses this project may move to next. */
    public Set<ProjectStatus> nextStatuses() {
        return EnumSet.copyOf(TRANSITIONS.get(status).isEmpty()
                ? EnumSet.noneOf(ProjectStatus.class) : TRANSITIONS.get(status));
    }

    public void schedule(LocalDate start, LocalDate end) {
        if (!end.isAfter(start)) {
            throw BusinessRuleException.forField("endDate", "project.dates.order");
        }
        this.startDate = start;
        this.endDate = end;
    }

    public boolean covers(LocalDate from, LocalDate to) {
        return !from.isBefore(startDate) && !to.isAfter(endDate);
    }

    public void ensureEditable() {
        if (status == ProjectStatus.ARCHIVED) {
            throw new BusinessRuleException("project.archived");
        }
    }

    public void updateProgress(int value) {
        this.progress = Math.max(0, Math.min(100, value));
    }

    public boolean isManagedBy(Long userId) {
        return manager.getId().equals(userId);
    }

    public void updateDetails(String name, String description, String location, BigDecimal latitude,
                              BigDecimal longitude, BigDecimal budget) {
        this.name = name;
        this.description = description;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.budget = budget;
    }

    public void changeClient(Client client) {
        this.client = client;
    }

    public void changeManager(User manager) {
        this.manager = manager;
    }

    public void changeCoverPhoto(StoredFile coverPhoto) {
        this.coverPhoto = coverPhoto;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Client getClient() {
        return client;
    }

    public User getManager() {
        return manager;
    }

    public String getLocation() {
        return location;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
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

    public ProjectStatus getStatus() {
        return status;
    }

    public int getProgress() {
        return progress;
    }

    public StoredFile getCoverPhoto() {
        return coverPhoto;
    }

    public boolean hasCoverPhoto() {
        return coverPhoto != null;
    }

    public LocalDate getCompletedDate() {
        return completedDate;
    }

    public boolean isActive() {
        return active;
    }
}
