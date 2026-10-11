package com.sitemaster.dailyreport;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.file.StoredFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A site photo attached to a daily report.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable).
 */
@Entity
@Table(name = "daily_report_photos")
public class DailyReportPhoto extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_id", nullable = false, updatable = false)
    private DailyReport report;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false, updatable = false)
    private StoredFile file;

    @Column(name = "caption", length = 150, updatable = false)
    private String caption;

    protected DailyReportPhoto() {
        // required by JPA
    }

    public DailyReportPhoto(DailyReport report, StoredFile file, String caption) {
        this.report = report;
        this.file = file;
        this.caption = caption;
    }

    public DailyReport getReport() {
        return report;
    }

    public StoredFile getFile() {
        return file;
    }

    public String getCaption() {
        return caption;
    }
}
