package com.sitemaster.project;

import com.sitemaster.project.dto.MilestoneCompleteRequest;
import com.sitemaster.project.dto.MilestoneRequest;
import com.sitemaster.project.dto.MilestoneResponse;
import com.sitemaster.project.dto.PhaseRequest;
import com.sitemaster.project.dto.PhaseResponse;
import com.sitemaster.project.dto.StatusChangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Phases and milestones. Changes are limited to the Admin and the project's PM in the service. */
@RestController
public class ProjectPlanController {

    private final ProjectPlanService planService;

    public ProjectPlanController(ProjectPlanService planService) {
        this.planService = planService;
    }

    @GetMapping("/api/projects/{projectId}/phases")
    @PreAuthorize("hasAuthority('PROJECT:VIEW')")
    public List<PhaseResponse> phases(@PathVariable Long projectId) {
        return planService.phases(projectId);
    }

    @PostMapping("/api/projects/{projectId}/phases")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public PhaseResponse createPhase(@PathVariable Long projectId, @Valid @RequestBody PhaseRequest request) {
        return planService.createPhase(projectId, request);
    }

    @PutMapping("/api/phases/{phaseId}")
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public PhaseResponse updatePhase(@PathVariable Long phaseId, @Valid @RequestBody PhaseRequest request) {
        return planService.updatePhase(phaseId, request);
    }

    @PatchMapping("/api/phases/{phaseId}/status")
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public PhaseResponse changePhaseStatus(@PathVariable Long phaseId, @Valid @RequestBody StatusChangeRequest request) {
        return planService.changePhaseStatus(phaseId, request.getStatus());
    }

    @DeleteMapping("/api/phases/{phaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public void deletePhase(@PathVariable Long phaseId) {
        planService.deletePhase(phaseId);
    }

    @GetMapping("/api/projects/{projectId}/milestones")
    @PreAuthorize("hasAuthority('PROJECT:VIEW')")
    public List<MilestoneResponse> milestones(@PathVariable Long projectId) {
        return planService.milestones(projectId);
    }

    @PostMapping("/api/projects/{projectId}/milestones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public MilestoneResponse createMilestone(@PathVariable Long projectId, @Valid @RequestBody MilestoneRequest request) {
        return planService.createMilestone(projectId, request);
    }

    @PutMapping("/api/milestones/{milestoneId}")
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public MilestoneResponse updateMilestone(@PathVariable Long milestoneId, @Valid @RequestBody MilestoneRequest request) {
        return planService.updateMilestone(milestoneId, request);
    }

    @PatchMapping("/api/milestones/{milestoneId}/complete")
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public MilestoneResponse completeMilestone(@PathVariable Long milestoneId,
                                               @Valid @RequestBody MilestoneCompleteRequest request) {
        return planService.completeMilestone(milestoneId, request.getCompleted());
    }

    @DeleteMapping("/api/milestones/{milestoneId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROJECT:EDIT')")
    public void deleteMilestone(@PathVariable Long milestoneId) {
        planService.deleteMilestone(milestoneId);
    }
}
