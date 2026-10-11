package com.sitemaster.document.dto;

import java.time.LocalDateTime;

/** One version of a project document. */
public record DocumentVersionResponse(Long id,
                                      int versionNo,
                                      String fileName,
                                      String contentType,
                                      long sizeBytes,
                                      String notes,
                                      String uploadedByName,
                                      LocalDateTime uploadedAt) {
}
