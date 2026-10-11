package com.sitemaster.file;

import com.sitemaster.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Metadata of an uploaded file. The bytes live in the upload directory under
 * {@link #getStoredName()} (a UUID), never under the user's original name.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable
 * after creation - there are no setters).
 */
@Entity
@Table(name = "stored_files")
public class StoredFile extends BaseEntity {

    @Column(name = "stored_name", nullable = false, unique = true, length = 64, updatable = false)
    private String storedName;

    @Column(name = "original_name", nullable = false, length = 255, updatable = false)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 150, updatable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20, updatable = false)
    private FileCategory category;

    protected StoredFile() {
        // required by JPA
    }

    public StoredFile(String storedName, String originalName, String contentType, long sizeBytes, FileCategory category) {
        this.storedName = storedName;
        this.originalName = originalName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.category = category;
    }

    public String getStoredName() {
        return storedName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public FileCategory getCategory() {
        return category;
    }
}
