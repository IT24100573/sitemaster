package com.sitemaster.document;

import com.sitemaster.common.enums.DocumentCategory;
import com.sitemaster.document.dto.DocumentResponse;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileResponses;
import com.sitemaster.validation.ValidFile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Project documents and their versions. */
@RestController
public class ProjectDocumentController {

    private final ProjectDocumentService documentService;

    public ProjectDocumentController(ProjectDocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/api/projects/{projectId}/documents")
    @PreAuthorize("hasAuthority('DOCUMENT:VIEW')")
    public List<DocumentResponse> list(@PathVariable Long projectId,
                                       @RequestParam(required = false) DocumentCategory category,
                                       @RequestParam(required = false) @Size(max = 100, message = "{search.size}") String search) {
        return documentService.list(projectId, category, search);
    }

    @PostMapping(value = "/api/projects/{projectId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('DOCUMENT:CREATE')")
    public DocumentResponse create(@PathVariable Long projectId,
                                   @RequestParam("title") @NotBlank(message = "{field.required}")
                                   @Size(min = 2, max = 150, message = "{title.size150}") String title,
                                   @RequestParam("category") @NotNull(message = "{field.required}") DocumentCategory category,
                                   @RequestParam(value = "description", required = false)
                                   @Size(max = 500, message = "{note.size}") String description,
                                   @RequestPart("file") @ValidFile(category = FileCategory.DOCUMENT) MultipartFile file) {
        return documentService.create(projectId, title, category, description, file);
    }

    @PostMapping(value = "/api/documents/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DOCUMENT:EDIT')")
    public DocumentResponse addVersion(@PathVariable Long documentId,
                                       @RequestParam(value = "notes", required = false)
                                       @Size(max = 500, message = "{note.size}") String notes,
                                       @RequestPart("file") @ValidFile(category = FileCategory.DOCUMENT) MultipartFile file) {
        return documentService.addVersion(documentId, file, notes);
    }

    @GetMapping("/api/document-versions/{versionId}/download")
    @PreAuthorize("hasAuthority('DOCUMENT:VIEW')")
    public ResponseEntity<Resource> download(@PathVariable Long versionId) {
        return FileResponses.attachment(documentService.openVersion(versionId));
    }

    @DeleteMapping("/api/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('DOCUMENT:DELETE')")
    public void delete(@PathVariable Long documentId) {
        documentService.delete(documentId);
    }
}
