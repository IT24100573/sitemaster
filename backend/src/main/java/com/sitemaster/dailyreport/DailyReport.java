package com.sitemaster.dailyreport;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.WeatherCondition;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.common.exception.UnauthorizedActionException;
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
import java.util.Objects;

/**
 * A site engineer's end-of-day report, reviewed by the project's PM.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - only the
 * author edits it (while pending or rejected; editing resubmits it), only
 * PENDING reports are reviewed, at most {@value #MAX_PHOTOS} photos are attached.
 */
@Entity
@Table(name = "daily_reports")
public class DailyReport extends BaseEntity {

    public static final int MAX_PHOTOS = 5;
    public static final int MAX_AGE_DAYS = 7;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by_id", nullable = false, updatable = false)
    private User submittedBy;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "work_done", nullable = false, length = 2000)
    private String workDone;

    @Column(name = "worker_count", nullable = false)
    private int workerCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather", nullable = false, length = 20)
    private WeatherCondition weather;

    @Column(name = "problems", length = 2000)
    private String problems;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_remarks", length = 500)
    private String reviewRemarks;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DailyReportMaterial> materialsUsed = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<DailyReportPhoto> photos = new ArrayList<>();

    protected DailyReport() {
        // required by JPA
    }

    public DailyReport(Project project, User submittedBy, LocalDate reportDate) {
        this.project = project;
        this.submittedBy = submittedBy;
        this.reportDate = reportDate;
    }

    /** Fills in the report; resubmits it when it had been rejected. */
    public void write(String workDone, int workerCount, WeatherCondition weather, String problems) {
        if (status == RequestStatus.APPROVED) {
            throw new InvalidStatusTransitionException("dailyReport.approved");
        }
        this.workDone = workDone;
        this.workerCount = workerCount;
        this.weather = weather;
        this.problems = problems;
        if (status == RequestStatus.REJECTED) {
            status = RequestStatus.PENDING;
            reviewedBy = null;
            reviewedAt = null;
        }
    }

    public void replaceMaterials(List<DailyReportMaterial> items) {
        materialsUsed.clear();
        materialsUsed.addAll(items);
    }

    public void addPhoto(DailyReportPhoto photo) {
        if (photos.size() >= MAX_PHOTOS) {
            throw BusinessRuleException.forField("file", "dailyReport.photos.max", MAX_PHOTOS);
        }
        photos.add(photo);
    }

    public void removePhoto(DailyReportPhoto photo) {
        photos.remove(photo);
    }

    public void approve(User reviewer, String remarks, LocalDateTime now) {
        review(reviewer, now);
        status = RequestStatus.APPROVED;
        reviewRemarks = remarks;
    }

    public void reject(User reviewer, String remarks, LocalDateTime now) {
        if (remarks == null || remarks.trim().length() < 5) {
            throw BusinessRuleException.forField("remarks", "request.reject.remarksRequired", 5);
        }
        review(reviewer, now);
        status = RequestStatus.REJECTED;
        reviewRemarks = remarks.trim();
    }

    private void review(User reviewer, LocalDateTime now) {
        if (status != RequestStatus.PENDING) {
            throw new InvalidStatusTransitionException("request.notPending");
        }
        if (Objects.equals(reviewer.getId(), submittedBy.getId())) {
            throw new UnauthorizedActionException("request.approve.own");
        }
        reviewedBy = reviewer;
        reviewedAt = now;
    }

    public boolean isAuthor(Long userId) {
        return submittedBy.getId().equals(userId);
    }

    public boolean isEditable() {
        return status != RequestStatus.APPROVED;
    }

    public Project getProject() {
        return project;
    }

    public User getSubmittedBy() {
        return submittedBy;
    }

    public LocalDate getReportDate() {
        return reportDate;
    }

    public String getWorkDone() {
        return workDone;
    }

    public int getWorkerCount() {
        return workerCount;
    }

    public WeatherCondition getWeather() {
        return weather;
    }

    public String getProblems() {
        return problems;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public String getReviewRemarks() {
        return reviewRemarks;
    }

    public List<DailyReportMaterial> getMaterialsUsed() {
        return Collections.unmodifiableList(materialsUsed);
    }

    public List<DailyReportPhoto> getPhotos() {
        return Collections.unmodifiableList(photos);
    }
}
