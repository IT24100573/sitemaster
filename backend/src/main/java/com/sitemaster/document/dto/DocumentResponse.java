package com.sitemaster.document.dto;

import java.time.LocalDateTime;
import java.util.List;

/** A project document with all its versions (newest first). */
public record DocumentResponse(Long id,
                               String title,
                               String category,
                               String description,
                               int currentVersion,
                               List<DocumentVersionResponse> versions,
                               LocalDateTime updatedAt) {

    public DocumentResponse {
        versions = List.copyOf(versions);
    }
}
