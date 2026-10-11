package com.sitemaster.employee;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.file.StoredFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A file on an employee's record (NIC copy, certificate...).
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable).
 */
@Entity
@Table(name = "employee_documents")
public class EmployeeDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 20, updatable = false)
    private EmployeeDocumentType documentType;

    @Column(name = "title", nullable = false, length = 100, updatable = false)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false, updatable = false)
    private StoredFile file;

    protected EmployeeDocument() {
        // required by JPA
    }

    public EmployeeDocument(Employee employee, EmployeeDocumentType documentType, String title, StoredFile file) {
        this.employee = employee;
        this.documentType = documentType;
        this.title = title;
        this.file = file;
    }

    public Employee getEmployee() {
        return employee;
    }

    public EmployeeDocumentType getDocumentType() {
        return documentType;
    }

    public String getTitle() {
        return title;
    }

    public StoredFile getFile() {
        return file;
    }
}
