package com.sitemaster.project;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.ProjectStatus;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileResponses;
import com.sitemaster.project.dto.ProjectDetail;
import com.sitemaster.project.dto.ProjectRequest;
import com.sitemaster.project.dto.ProjectSummary;
import com.sitemaster.project.dto.StatusChangeRequest;
import com.sitemaster.validation.ValidFile;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Projects. Visibility and "own project" checks happen in ProjectService. */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROJECT:VIEW')")
    public PageResponse<ProjectSummary> list(@RequestParam(required = false) ProjectStatus status,
                                             @Valid @ModelAttribute PageQuery pageQuery) {
        return projectService.search(status, pageQuery);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT:VIEW')")
    public ProjectDetail get(@PathVariable Long id) {
        return projectService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROJECT:CREATE')")
    public ProjectDetail create(@Valid @RequestBody ProjectRequest request) {
        return projectService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public ProjectDetail update(@PathVariable Long id, @Valid @RequestBody ProjectRequest request) {
        return projectService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public ProjectDetail changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return projectService.changeStatus(id, request.getStatus());
    }

    @PostMapping(value = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public ProjectDetail uploadCover(@PathVariable Long id,
                                     @RequestPart("file") @ValidFile(category = FileCategory.IMAGE) MultipartFile file) {
        return projectService.changeCover(id, file);
    }

    @DeleteMapping("/{id}/cover")
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public ProjectDetail removeCover(@PathVariable Long id) {
        return projectService.removeCover(id);
    }

    @GetMapping("/{id}/cover")
    @PreAuthorize("hasAuthority('PROJECT:VIEW')")
    public ResponseEntity<Resource> cover(@PathVariable Long id) {
        return FileResponses.inline(projectService.openCover(id));
    }
}
