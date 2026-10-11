package com.sitemaster.project;

import com.sitemaster.project.dto.MilestoneRequest;
import com.sitemaster.project.dto.MilestoneResponse;
import com.sitemaster.project.dto.PhaseRequest;
import com.sitemaster.project.dto.PhaseResponse;

import java.util.List;

/**
 * Phases and milestones of a project (the PM's plan).
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface ProjectPlanService {

    List<PhaseResponse> phases(Long projectId);

    PhaseResponse createPhase(Long projectId, PhaseRequest request);

    PhaseResponse updatePhase(Long phaseId, PhaseRequest request);

    PhaseResponse changePhaseStatus(Long phaseId, String status);

    void deletePhase(Long phaseId);

    List<MilestoneResponse> milestones(Long projectId);

    MilestoneResponse createMilestone(Long projectId, MilestoneRequest request);

    MilestoneResponse updateMilestone(Long milestoneId, MilestoneRequest request);

    MilestoneResponse completeMilestone(Long milestoneId, boolean completed);

    void deleteMilestone(Long milestoneId);
}
