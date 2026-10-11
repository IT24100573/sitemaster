package com.sitemaster.document;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.file.StoredFile;
import com.sitemaster.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One uploaded version of a project document. Versions are never changed.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable).
 */
@Entity
@Table(name = "project_document_versions")
public class ProjectDocumentVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false, updatable = false)
    private ProjectDocument document;

    @Column(name = "version_no", nullable = false, updatable = false)
    private int versionNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false, updatable = false)
    private StoredFile file;

    @Column(name = "notes", length = 500, updatable = false)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_id", nullable = false, updatable = false)
    private User uploadedBy;

    protected ProjectDocumentVersion() {
        // required by JPA
    }

    ProjectDocumentVersion(ProjectDocument document, int versionNo, StoredFile file, String notes, User uploadedBy) {
        this.document = document;
        this.versionNo = versionNo;
        this.file = file;
        this.notes = notes;
        this.uploadedBy = uploadedBy;
    }

    public ProjectDocument getDocument() {
        return document;
    }

    public int getVersionNo() {
        return versionNo;
    }

    public StoredFile getFile() {
        return file;
    }

    public String getNotes() {
        return notes;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }
}
