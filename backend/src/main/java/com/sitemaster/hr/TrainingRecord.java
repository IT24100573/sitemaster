package com.sitemaster.hr;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.employee.Employee;
import com.sitemaster.file.StoredFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * A training course or safety certification an employee completed.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - the
 * certificate expiry cannot be before completion.
 */
@Entity
@Table(name = "training_records")
public class TrainingRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "provider", length = 150)
    private String provider;

    @Column(name = "safety", nullable = false)
    private boolean safety;

    @Column(name = "completed_on", nullable = false)
    private LocalDate completedOn;

    @Column(name = "expires_on")
    private LocalDate expiresOn;

    @Column(name = "notes", length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private StoredFile file;

    protected TrainingRecord() {
        // required by JPA
    }

    public TrainingRecord(Employee employee) {
        this.employee = employee;
    }

    public void update(Employee employee, String title, String provider, boolean safety, LocalDate completedOn,
                       LocalDate expiresOn, String notes) {
        if (expiresOn != null && expiresOn.isBefore(completedOn)) {
            throw BusinessRuleException.forField("expiresOn", "daterange.invalid");
        }
        this.employee = employee;
        this.title = title;
        this.provider = provider;
        this.safety = safety;
        this.completedOn = completedOn;
        this.expiresOn = expiresOn;
        this.notes = notes;
    }

    public StoredFile replaceFile(StoredFile newFile) {
        StoredFile old = file;
        file = newFile;
        return old;
    }

    public boolean isExpired(LocalDate today) {
        return expiresOn != null && expiresOn.isBefore(today);
    }

    public Employee getEmployee() {
        return employee;
    }

    public String getTitle() {
        return title;
    }

    public String getProvider() {
        return provider;
    }

    public boolean isSafety() {
        return safety;
    }

    public LocalDate getCompletedOn() {
        return completedOn;
    }

    public LocalDate getExpiresOn() {
        return expiresOn;
    }

    public String getNotes() {
        return notes;
    }

    public StoredFile getFile() {
        return file;
    }
}
