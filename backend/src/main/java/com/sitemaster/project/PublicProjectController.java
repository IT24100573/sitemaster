package com.sitemaster.project;

import com.sitemaster.file.FileResponses;
import com.sitemaster.project.dto.ProjectMapMarker;
import com.sitemaster.project.dto.PublicProjectCard;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Public home-page gallery and sites map. Exposes no budgets, staff or clients' contact details. */
@RestController
@RequestMapping("/api/public/projects")
public class PublicProjectController {

    private final ProjectService projectService;

    public PublicProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping("/completed")
    public List<PublicProjectCard> completed() {
        return projectService.completedGallery();
    }

    @GetMapping("/map")
    public List<ProjectMapMarker> map() {
        return projectService.mapMarkers();
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<Resource> cover(@PathVariable Long id) {
        return FileResponses.inline(projectService.openPublicCover(id));
    }
}
