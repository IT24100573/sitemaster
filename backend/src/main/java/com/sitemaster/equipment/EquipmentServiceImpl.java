package com.sitemaster.equipment;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.EquipmentStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.equipment.dto.EquipmentConditionRequest;
import com.sitemaster.equipment.dto.EquipmentRequest;
import com.sitemaster.equipment.dto.EquipmentResponse;
import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.project.ProjectMember;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link EquipmentService}.
 * <ul>
 *   <li>Admin registers equipment and can assign it anywhere.</li>
 *   <li>A PM may take available equipment for, or release it from, their own projects.</li>
 *   <li>A site engineer on the project updates status and service dates.</li>
 *   <li>PMs and site engineers see equipment on their projects plus unassigned equipment.</li>
 * </ul>
 * OOP concept: Abstraction - implements EquipmentService.
 */
@Service
public class EquipmentServiceImpl implements EquipmentService {

    private static final Map<String, String> SORTS = Map.of(
            "code", "code", "name", "name", "status", "status", "nextServiceDate", "nextServiceDate");

    private final EquipmentRepository equipmentRepository;
    private final ProjectAccess access;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public EquipmentServiceImpl(EquipmentRepository equipmentRepository, ProjectAccess access,
                                CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher, Clock clock) {
        this.equipmentRepository = equipmentRepository;
        this.access = access;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EquipmentResponse> search(Long projectId, EquipmentStatus status, PageQuery pageQuery) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        if (projectId != null) {
            access.requireVisible(projectId);
        }
        String search = pageQuery.getSearch();
        Specification<Equipment> spec = (root, query, cb) -> {
            if (me.getRole() == RoleName.ADMIN || me.getRole() == RoleName.ACCOUNTANT) {
                return null;
            }
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ProjectMember> member = sub.from(ProjectMember.class);
            sub.select(member.get("project").get("id")).where(cb.equal(member.get("user").get("id"), me.getUserId()));
            return cb.or(cb.isNull(root.get("project")), root.get("project").get("id").in(sub));
        };
        spec = spec.and((root, query, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId))
                .and((root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status))
                .and((root, query, cb) -> {
                    if (search == null) {
                        return null;
                    }
                    String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                    return cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("code")), like),
                            cb.like(cb.lower(root.get("category")), like));
                });
        return PageResponse.from(equipmentRepository.findAll(spec, pageQuery.toPageable(SORTS, "code", Sort.Direction.ASC)),
                this::toResponse);
    }

    @Override
    @Transactional
    public EquipmentResponse create(EquipmentRequest request) {
        if (equipmentRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw DuplicateRecordException.forField("code", "equipment.code.duplicate");
        }
        Equipment equipment = new Equipment(request.getCode());
        equipment.update(request.getCode(), request.getName(), request.getCategory());
        equipmentRepository.save(equipment);
        publish(WorkflowEventType.EQUIPMENT_SAVED, equipment, null, equipment.getCode(), equipment.getName());
        return toResponse(equipment);
    }

    @Override
    @Transactional
    public EquipmentResponse update(Long id, EquipmentRequest request) {
        Equipment equipment = find(id);
        if (equipmentRepository.existsByCodeIgnoreCaseAndIdNot(request.getCode(), id)) {
            throw DuplicateRecordException.forField("code", "equipment.code.duplicate");
        }
        equipment.update(request.getCode(), request.getName(), request.getCategory());
        publish(WorkflowEventType.EQUIPMENT_SAVED, equipment, null, equipment.getCode(), equipment.getName());
        return toResponse(equipment);
    }

    @Override
    @Transactional
    public EquipmentResponse assign(Long id, Long projectId) {
        Equipment equipment = find(id);
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        boolean admin = me.getRole() == RoleName.ADMIN;
        // A PM may only release from, or assign to, projects they manage.
        if (!admin) {
            if (equipment.getProject() != null) {
                access.requireManageable(equipment.getProject().getId());
            }
            if (projectId != null && equipment.getProject() != null && !equipment.getProject().getId().equals(projectId)) {
                throw new UnauthorizedActionException("equipment.assign.busy");
            }
        }
        Project target = projectId == null ? null
                : (admin ? access.requireVisible(projectId) : access.requireManageable(projectId));
        if (target != null) {
            target.ensureEditable();
        }
        equipment.assignTo(target);
        publish(WorkflowEventType.EQUIPMENT_ASSIGNED, equipment,
                target == null ? null : target.getManager().getId(),
                equipment.getCode(), target == null ? "-" : target.getName());
        return toResponse(equipment);
    }

    @Override
    @Transactional
    public EquipmentResponse updateCondition(Long id, EquipmentConditionRequest request) {
        Equipment equipment = find(id);
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        if (me.getRole() != RoleName.ADMIN) {
            if (equipment.getProject() == null) {
                throw new UnauthorizedActionException("equipment.condition.denied");
            }
            Project project = access.requireParticipant(equipment.getProject().getId());
            if (!access.isManager(me, project) && !access.isMember(project)) {
                throw new UnauthorizedActionException("equipment.condition.denied");
            }
        }
        equipment.updateCondition(request.getStatus(), request.getNotes(), request.getLastServiceDate(),
                request.getNextServiceDate());
        publish(WorkflowEventType.EQUIPMENT_STATUS_CHANGED, equipment, null, equipment.getCode(), equipment.getStatus());
        return toResponse(equipment);
    }

    private void publish(WorkflowEventType type, Equipment equipment, Long recipient, Object... args) {
        WorkflowEvent.Builder builder = WorkflowEvent.byCurrentUser(type).entityId(equipment.getId()).args(args)
                .link("equipment");
        if (recipient != null && !recipient.equals(currentUser.requireId())) {
            builder.recipients(recipient);
        }
        eventPublisher.publish(builder.build());
    }

    private Equipment find(Long id) {
        return equipmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Equipment"));
    }

    private EquipmentResponse toResponse(Equipment e) {
        boolean due = e.getNextServiceDate() != null && !e.getNextServiceDate().isAfter(LocalDate.now(clock))
                && e.getStatus() != EquipmentStatus.RETIRED;
        return new EquipmentResponse(e.getId(), e.getCode(), e.getName(), e.getCategory(),
                e.getProject() == null ? null : e.getProject().getId(),
                e.getProject() == null ? null : e.getProject().getName(), e.getStatus().name(), e.getNotes(),
                e.getLastServiceDate(), e.getNextServiceDate(), due);
    }
}
