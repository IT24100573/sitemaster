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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * An employment contract with its period. HR is reminded before it ends.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - the status
 * (upcoming, active, expired) is derived from the dates, never stored; changing
 * the end date re-arms the renewal reminder.
 */
@Entity
@Table(name = "contracts")
public class Contract extends BaseEntity {

    /** Lifecycle derived from the dates. */
    public enum Status {
        UPCOMING,
        ACTIVE,
        EXPIRED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "contract_no", nullable = false, unique = true, length = 30)
    private String contractNo;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "contract_value", precision = 14, scale = 2)
    private BigDecimal contractValue;

    @Column(name = "notes", length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private StoredFile file;

    @Column(name = "reminder_sent_at")
    private LocalDateTime reminderSentAt;

    protected Contract() {
        // required by JPA
    }

    public Contract(Employee employee) {
        this.employee = employee;
    }

    public void update(Employee employee, String contractNo, String title, LocalDate start, LocalDate end,
                       BigDecimal value, String notes) {
        if (end.isBefore(start)) {
            throw BusinessRuleException.forField("endDate", "daterange.invalid");
        }
        if (this.endDate != null && !this.endDate.equals(end)) {
            reminderSentAt = null;
        }
        this.employee = employee;
        this.contractNo = contractNo.trim().toUpperCase(Locale.ROOT);
        this.title = title;
        this.startDate = start;
        this.endDate = end;
        this.contractValue = value;
        this.notes = notes;
    }

    /** @return the replaced file, to delete from storage */
    public StoredFile replaceFile(StoredFile newFile) {
        StoredFile old = file;
        file = newFile;
        return old;
    }

    public Status statusOn(LocalDate today) {
        if (today.isBefore(startDate)) {
            return Status.UPCOMING;
        }
        return today.isAfter(endDate) ? Status.EXPIRED : Status.ACTIVE;
    }

    public long daysToExpiry(LocalDate today) {
        return ChronoUnit.DAYS.between(today, endDate);
    }

    public void markReminded(LocalDateTime now) {
        reminderSentAt = now;
    }

    public Employee getEmployee() {
        return employee;
    }

    public String getContractNo() {
        return contractNo;
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public BigDecimal getContractValue() {
        return contractValue;
    }

    public String getNotes() {
        return notes;
    }

    public StoredFile getFile() {
        return file;
    }

    public LocalDateTime getReminderSentAt() {
        return reminderSentAt;
    }
}
