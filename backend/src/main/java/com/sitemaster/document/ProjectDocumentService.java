package com.sitemaster.document;

import com.sitemaster.common.enums.DocumentCategory;
import com.sitemaster.document.dto.DocumentResponse;
import com.sitemaster.file.FileDownload;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Versioned project documents.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface ProjectDocumentService {

    List<DocumentResponse> list(Long projectId, DocumentCategory category, String search);

    DocumentResponse create(Long projectId, String title, DocumentCategory category, String description,
                            MultipartFile file);

    DocumentResponse addVersion(Long documentId, MultipartFile file, String notes);

    FileDownload openVersion(Long versionId);

    void delete(Long documentId);
}
