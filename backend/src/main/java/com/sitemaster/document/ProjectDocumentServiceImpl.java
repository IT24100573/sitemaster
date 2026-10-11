package com.sitemaster.document;

import com.sitemaster.common.enums.DocumentCategory;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.document.dto.DocumentResponse;
import com.sitemaster.document.dto.DocumentVersionResponse;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileDownload;
import com.sitemaster.file.FileStorageService;
import com.sitemaster.file.StoredFile;
import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

/**
 * Implementation of {@link ProjectDocumentService}. Team members who can see the
 * project may read documents; only the Admin or the project's PM uploads, adds
 * versions or deletes. Earlier versions are always kept.
 * <p>
 * OOP concept: Abstraction - implements ProjectDocumentService.
 */
@Service
public class ProjectDocumentServiceImpl implements ProjectDocumentService {

    private final ProjectDocumentRepository documentRepository;
    private final ProjectDocumentVersionRepository versionRepository;
    private final FileStorageService fileStorage;
    private final ProjectAccess access;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;

    public ProjectDocumentServiceImpl(ProjectDocumentRepository documentRepository,
                                      ProjectDocumentVersionRepository versionRepository,
                                      FileStorageService fileStorage, ProjectAccess access,
                                      CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.versionRepository = versionRepository;
        this.fileStorage = fileStorage;
        this.access = access;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> list(Long projectId, DocumentCategory category, String search) {
        access.requireVisible(projectId);
        String needle = search == null ? null : search.toLowerCase(Locale.ROOT);
        return documentRepository.findByProjectIdOrderByTitleAsc(projectId).stream()
                .filter(d -> category == null || d.getCategory() == category)
                .filter(d -> needle == null || d.getTitle().toLowerCase(Locale.ROOT).contains(needle))
                .map(ProjectDocumentServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DocumentResponse create(Long projectId, String title, DocumentCategory category, String description,
                                   MultipartFile file) {
        Project project = access.requireManageable(projectId);
        StoredFile stored = fileStorage.store(file, FileCategory.DOCUMENT, "file");
        ProjectDocument document = new ProjectDocument(project, title, category, description);
        document.addVersion(stored, null, currentUser.requireUser());
        documentRepository.saveAndFlush(document);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.DOCUMENT_UPLOADED)
                .entityId(document.getId()).args(title, project.getName()).build());
        return toResponse(document);
    }

    @Override
    @Transactional
    public DocumentResponse addVersion(Long documentId, MultipartFile file, String notes) {
        ProjectDocument document = find(documentId);
        Project project = access.requireManageable(document.getProject().getId());
        StoredFile stored = fileStorage.store(file, FileCategory.DOCUMENT, "file");
        document.addVersion(stored, notes, currentUser.requireUser());
        documentRepository.saveAndFlush(document);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.DOCUMENT_VERSION_ADDED)
                .entityId(document.getId()).args(document.getTitle(), document.getCurrentVersion(), project.getName())
                .build());
        return toResponse(document);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload openVersion(Long versionId) {
        ProjectDocumentVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Document"));
        access.requireVisible(version.getDocument().getProject().getId());
        return fileStorage.open(version.getFile());
    }

    @Override
    @Transactional
    public void delete(Long documentId) {
        ProjectDocument document = find(documentId);
        Project project = access.requireManageable(document.getProject().getId());
        List<StoredFile> files = document.getVersions().stream().map(ProjectDocumentVersion::getFile).toList();
        String title = document.getTitle();
        documentRepository.delete(document);
        documentRepository.flush();
        files.forEach(fileStorage::delete);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.DOCUMENT_DELETED)
                .entityId(documentId).args(title, project.getName()).build());
    }

    private ProjectDocument find(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Document"));
    }

    private static DocumentResponse toResponse(ProjectDocument d) {
        return new DocumentResponse(d.getId(), d.getTitle(), d.getCategory().name(), d.getDescription(),
                d.getCurrentVersion(),
                d.getVersions().stream().map(v -> new DocumentVersionResponse(v.getId(), v.getVersionNo(),
                        v.getFile().getOriginalName(), v.getFile().getContentType(), v.getFile().getSizeBytes(),
                        v.getNotes(), v.getUploadedBy().getFullName(), v.getCreatedAt())).toList(),
                d.getUpdatedAt());
    }
}
