package com.sitemaster.finance;

import com.sitemaster.finance.dto.ProjectFinanceResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Project financials (Admin, Accountant, the project's PM). */
@RestController
public class ProjectFinanceController {

    private final ProjectFinanceService service;

    public ProjectFinanceController(ProjectFinanceService service) {
        this.service = service;
    }

    @GetMapping("/api/projects/{projectId}/finance")
    @PreAuthorize("hasAuthority('PROJECT:VIEW')")
    public ProjectFinanceResponse forProject(@PathVariable Long projectId) {
        return service.forProject(projectId);
    }

    @GetMapping("/api/finance/projects")
    @PreAuthorize("hasAuthority('PROJECT:VIEW')")
    public List<ProjectFinanceResponse> overview() {
        return service.overview();
    }
}
