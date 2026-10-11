package com.sitemaster.supplier;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.ApplicationStatus;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * A company's request, sent from the public website, to become a supplier.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - only a
 * PENDING application can be approved or rejected, and rejection needs remarks.
 */
@Entity
@Table(name = "supplier_applications")
public class SupplierApplication extends BaseEntity {

    public static final int MIN_REMARKS_LENGTH = 5;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "registration_no", nullable = false, length = 50)
    private String registrationNo;

    @Column(name = "contact_person", nullable = false, length = 100)
    private String contactPerson;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "material_categories", nullable = false, length = 1000)
    private String materialCategories;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_document_id", nullable = false, updatable = false)
    private StoredFile registrationDocument;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_remarks", length = 500)
    private String reviewRemarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    protected SupplierApplication() {
        // required by JPA
    }

    public SupplierApplication(String companyName, String registrationNo, String contactPerson, String email,
                               String phone, String address, List<String> categories, StoredFile registrationDocument) {
        this.companyName = companyName;
        this.registrationNo = registrationNo.trim().toUpperCase(Locale.ROOT);
        this.contactPerson = contactPerson;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.phone = phone;
        this.address = address;
        this.materialCategories = String.join(Supplier.SEPARATOR, categories);
        this.registrationDocument = registrationDocument;
    }

    public void approve(User reviewer, Supplier createdSupplier, String remarks, LocalDateTime now) {
        ensurePending();
        status = ApplicationStatus.APPROVED;
        reviewedBy = reviewer;
        reviewedAt = now;
        reviewRemarks = remarks;
        supplier = createdSupplier;
    }

    public void reject(User reviewer, String remarks, LocalDateTime now) {
        ensurePending();
        if (remarks == null || remarks.trim().length() < MIN_REMARKS_LENGTH) {
            throw BusinessRuleException.forField("remarks", "request.reject.remarksRequired", MIN_REMARKS_LENGTH);
        }
        status = ApplicationStatus.REJECTED;
        reviewedBy = reviewer;
        reviewedAt = now;
        reviewRemarks = remarks.trim();
    }

    private void ensurePending() {
        if (status != ApplicationStatus.PENDING) {
            throw new InvalidStatusTransitionException("supplierApplication.notPending");
        }
    }

    public List<String> getMaterialCategories() {
        return Arrays.stream(materialCategories.split(Supplier.SEPARATOR)).map(String::trim).toList();
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getRegistrationNo() {
        return registrationNo;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public StoredFile getRegistrationDocument() {
        return registrationDocument;
    }

    public ApplicationStatus getStatus() {
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

    public Supplier getSupplier() {
        return supplier;
    }
}
