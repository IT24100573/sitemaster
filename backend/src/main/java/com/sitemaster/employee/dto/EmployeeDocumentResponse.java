package com.sitemaster.employee.dto;

import java.time.LocalDateTime;

/**
 * A document on an employee's file.
 *
 * @param id           document id
 * @param documentType NIC, CERTIFICATE or OTHER
 * @param title        description entered by HR
 * @param fileName     original file name
 * @param contentType  real MIME type
 * @param sizeBytes    file size
 * @param uploadedAt   upload time
 */
public record EmployeeDocumentResponse(Long id,
                                       String documentType,
                                       String title,
                                       String fileName,
                                       String contentType,
                                       long sizeBytes,
                                       LocalDateTime uploadedAt) {
}
