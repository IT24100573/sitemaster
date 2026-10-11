package com.sitemaster.project;

import com.sitemaster.common.enums.PhaseStatus;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.util.EnumParser;
import com.sitemaster.project.dto.MilestoneRequest;
import com.sitemaster.project.dto.MilestoneResponse;
import com.sitemaster.project.dto.PhaseRequest;
import com.sitemaster.project.dto.PhaseResponse;
import com.sitemaster.task.TaskRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of {@link ProjectPlanService}. Rules: phase dates inside the
 * project; total phase budgets &lt;= project budget; a phase with open tasks cannot
 * be completed and a phase with tasks cannot be deleted; milestones fall inside
 * the project dates. Only the Admin or the project's PM may change the plan.
 * <p>
 * OOP concept: Abstraction - implements ProjectPlanService.
 */
@Service
public class ProjectPlanServiceImpl implements ProjectPlanService {

    private final ProjectAccess access;
    private final PhaseRepository phaseRepository;
    private final MilestoneRepository milestoneRepository;
    private final TaskRepository taskRepository;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public ProjectPlanServiceImpl(ProjectAccess access, PhaseRepository phaseRepository,
                                  MilestoneRepository milestoneRepository, TaskRepository taskRepository,
                                  WorkflowEventPublisher eventPublisher, Clock clock) {
        this.access = access;
        this.phaseRepository = phaseRepository;
        this.milestoneRepository = milestoneRepository;
        this.taskRepository = taskRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // ----- phases ----------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<PhaseResponse> phases(Long projectId) {
        access.requireVisible(projectId);
        return phaseRepository.findByProjectIdOrderBySortOrderAscStartDateAsc(projectId).stream()
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PhaseResponse createPhase(Long projectId, PhaseRequest request) {
        Project project = access.requireManageable(projectId);
        checkBudget(project, request.getBudget(), null);
        Phase phase = new Phase(project);
        apply(phase, request);
        phaseRepository.save(phase);
        publish(WorkflowEventType.PHASE_SAVED, project, phase.getId(), phase.getName(), project.getName());
        return toResponse(phase);
    }

    @Override
    @Transactional
    public PhaseResponse updatePhase(Long phaseId, PhaseRequest request) {
        Phase phase = findPhase(phaseId);
        Project project = access.requireManageable(phase.getProject().getId());
        checkBudget(project, request.getBudget(), phaseId);
        apply(phase, request);
        publish(WorkflowEventType.PHASE_SAVED, project, phase.getId(), phase.getName(), project.getName());
        return toResponse(phase);
    }

    @Override
    @Transactional
    public PhaseResponse changePhaseStatus(Long phaseId, String status) {
        PhaseStatus target = EnumParser.parse(PhaseStatus.class, status, "status");
        Phase phase = findPhase(phaseId);
        Project project = access.requireManageable(phase.getProject().getId());
        switch (target) {
            case COMPLETED -> phase.complete(taskRepository.countByPhaseIdAndStatusNot(phaseId, TaskStatus.DONE));
            case IN_PROGRESS -> phase.start();
            case NOT_STARTED -> {
                if (taskRepository.countByPhaseIdAndStatusNot(phaseId, TaskStatus.TODO) > 0) {
                    throw BusinessRuleException.forField("status", "phase.reset.started");
                }
                phase.reset();
            }
        }
        publish(WorkflowEventType.PHASE_STATUS_CHANGED, project, phase.getId(), phase.getName(), target);
        return toResponse(phase);
    }

    @Override
    @Transactional
    public void deletePhase(Long phaseId) {
        Phase phase = findPhase(phaseId);
        Project project = access.requireManageable(phase.getProject().getId());
        if (taskRepository.countByPhaseId(phaseId) > 0) {
            throw new BusinessRuleException("phase.delete.hasTasks");
        }
        phaseRepository.delete(phase);
        publish(WorkflowEventType.PHASE_DELETED, project, phaseId, phase.getName(), project.getName());
    }

    // ----- milestones ----------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<MilestoneResponse> milestones(Long projectId) {
        access.requireVisible(projectId);
        return milestoneRepository.findByProjectIdOrderByDueDateAsc(projectId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public MilestoneResponse createMilestone(Long projectId, MilestoneRequest request) {
        Project project = access.requireManageable(projectId);
        Milestone milestone = new Milestone(project);
        milestone.update(request.getName(), request.getDueDate(), resolvePhase(project, request.getPhaseId()));
        milestoneRepository.save(milestone);
        publish(WorkflowEventType.MILESTONE_SAVED, project, milestone.getId(), milestone.getName(), project.getName());
        return toResponse(milestone);
    }

    @Override
    @Transactional
    public MilestoneResponse updateMilestone(Long milestoneId, MilestoneRequest request) {
        Milestone milestone = findMilestone(milestoneId);
        Project project = access.requireManageable(milestone.getProject().getId());
        milestone.update(request.getName(), request.getDueDate(), resolvePhase(project, request.getPhaseId()));
        publish(WorkflowEventType.MILESTONE_SAVED, project, milestone.getId(), milestone.getName(), project.getName());
        return toResponse(milestone);
    }

    @Override
    @Transactional
    public MilestoneResponse completeMilestone(Long milestoneId, boolean completed) {
        Milestone milestone = findMilestone(milestoneId);
        Project project = access.requireManageable(milestone.getProject().getId());
        milestone.markCompleted(completed, LocalDate.now(clock));
        if (completed) {
            publish(WorkflowEventType.MILESTONE_COMPLETED, project, milestone.getId(), milestone.getName(),
                    project.getName());
        }
        return toResponse(milestone);
    }

    @Override
    @Transactional
    public void deleteMilestone(Long milestoneId) {
        Milestone milestone = findMilestone(milestoneId);
        Project project = access.requireManageable(milestone.getProject().getId());
        milestoneRepository.delete(milestone);
        publish(WorkflowEventType.MILESTONE_DELETED, project, milestoneId, milestone.getName(), project.getName());
    }

    // ----- helpers -------------------------------------------------------------------

    private void apply(Phase phase, PhaseRequest r) {
        phase.update(r.getName(), r.getDescription(), r.getStartDate(), r.getEndDate(), r.getBudget(),
                r.getSortOrder() == null ? 0 : r.getSortOrder());
        // Existing task due dates must stay inside the phase.
        if (phase.getId() != null) {
            taskRepository.findByPhaseId(phase.getId()).stream()
                    .filter(t -> !phase.covers(t.getDueDate()))
                    .findFirst()
                    .ifPresent(t -> {
                        throw BusinessRuleException.forField("startDate", "phase.dates.tasksOutside", t.getTitle());
                    });
        }
    }

    private void checkBudget(Project project, BigDecimal phaseBudget, Long excludePhaseId) {
        BigDecimal others = phaseRepository.sumBudget(project.getId(), excludePhaseId);
        BigDecimal remaining = project.getBudget().subtract(others);
        if (phaseBudget.compareTo(remaining) > 0) {
            throw BusinessRuleException.forField("budget", "phase.budget.exceeds", remaining);
        }
    }

    private Phase resolvePhase(Project project, Long phaseId) {
        if (phaseId == null) {
            return null;
        }
        Phase phase = findPhase(phaseId);
        if (!phase.getProject().getId().equals(project.getId())) {
            throw BusinessRuleException.forField("phaseId", "phase.otherProject");
        }
        return phase;
    }

    private Phase findPhase(Long id) {
        return phaseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Phase"));
    }

    private Milestone findMilestone(Long id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Milestone"));
    }

    private void publish(WorkflowEventType type, Project project, Long entityId, Object... args) {
        eventPublisher.publish(WorkflowEvent.byCurrentUser(type).entityId(entityId).args(args)
                .link("projects/" + project.getId()).build());
    }

    private PhaseResponse toResponse(Phase p) {
        return new PhaseResponse(p.getId(), p.getName(), p.getDescription(), p.getStartDate(), p.getEndDate(),
                p.getBudget(), p.getStatus().name(), p.getSortOrder(),
                p.getId() == null ? 0 : taskRepository.countByPhaseId(p.getId()),
                p.getId() == null ? 0 : taskRepository.countByPhaseIdAndStatusNot(p.getId(), TaskStatus.DONE));
    }

    private MilestoneResponse toResponse(Milestone m) {
        boolean overdue = !m.isCompleted() && m.getDueDate().isBefore(LocalDate.now(clock));
        return new MilestoneResponse(m.getId(), m.getName(), m.getDueDate(),
                m.getPhase() == null ? null : m.getPhase().getId(),
                m.getPhase() == null ? null : m.getPhase().getName(),
                m.isCompleted(), m.getCompletedDate(), overdue);
    }
}
