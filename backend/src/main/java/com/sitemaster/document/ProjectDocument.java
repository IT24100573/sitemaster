package com.sitemaster.document;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.DocumentCategory;
import com.sitemaster.file.StoredFile;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A project document (drawing, permit...) with a history of versions.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation - versions are
 * only added through {@link #addVersion}, which numbers them 1, 2, 3...
 */
@Entity
@Table(name = "project_documents")
public class ProjectDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false, updatable = false)
    private Project project;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private DocumentCategory category;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "current_version", nullable = false)
    private int currentVersion;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("versionNo DESC")
    private List<ProjectDocumentVersion> versions = new ArrayList<>();

    protected ProjectDocument() {
        // required by JPA
    }

    public ProjectDocument(Project project, String title, DocumentCategory category, String description) {
        this.project = project;
        this.title = title;
        this.category = category;
        this.description = description;
    }

    public ProjectDocumentVersion addVersion(StoredFile file, String notes, User uploadedBy) {
        currentVersion++;
        ProjectDocumentVersion version = new ProjectDocumentVersion(this, currentVersion, file, notes, uploadedBy);
        versions.add(0, version);
        return version;
    }

    public ProjectDocumentVersion latestVersion() {
        return versions.isEmpty() ? null : versions.get(0);
    }

    public Project getProject() {
        return project;
    }

    public String getTitle() {
        return title;
    }

    public DocumentCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }

    public List<ProjectDocumentVersion> getVersions() {
        return Collections.unmodifiableList(versions);
    }
}
