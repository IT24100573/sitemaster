package com.sitemaster.project;

import com.sitemaster.common.dto.LookupOption;
import com.sitemaster.project.dto.AddMemberRequest;
import com.sitemaster.project.dto.MemberResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Project team assignment. */
@RestController
@RequestMapping("/api/projects/{projectId}/members")
public class ProjectTeamController {

    private final ProjectTeamService teamService;

    public ProjectTeamController(ProjectTeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROJECT:VIEW')")
    public List<MemberResponse> members(@PathVariable Long projectId) {
        return teamService.members(projectId);
    }

    @GetMapping("/candidates")
    @PreAuthorize("hasAuthority('PROJECT:VIEW')")
    public List<LookupOption> candidates(@PathVariable Long projectId) {
        return teamService.candidates(projectId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PROJECT:VIEW')")
    public MemberResponse add(@PathVariable Long projectId, @Valid @RequestBody AddMemberRequest request) {
        return teamService.add(projectId, request.getUserId());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('PROJECT:VIEW')")
    public void remove(@PathVariable Long projectId, @PathVariable Long userId) {
        teamService.remove(projectId, userId);
    }
}
