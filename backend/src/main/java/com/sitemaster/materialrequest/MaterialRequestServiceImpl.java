package com.sitemaster.materialrequest;

import com.sitemaster.budget.PhaseBudgetService;
import com.sitemaster.budget.dto.BudgetCheckResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.PhaseStatus;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.material.Material;
import com.sitemaster.material.MaterialRepository;
import com.sitemaster.material.SiteStock;
import com.sitemaster.material.SiteStockRepository;
import com.sitemaster.materialrequest.dto.DeliveryRequest;
import com.sitemaster.materialrequest.dto.MaterialRequestCreate;
import com.sitemaster.materialrequest.dto.MaterialRequestResponse;
import com.sitemaster.project.Phase;
import com.sitemaster.project.PhaseRepository;
import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.purchase.PurchaseOrder;
import com.sitemaster.purchase.PurchaseOrderRepository;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link MaterialRequestService}. Rules:
 * <ul>
 *   <li>only a site engineer on the project creates requests; quantity &gt; 0,
 *       required-by today or later, phase of this project and not completed;</li>
 *   <li>only PENDING requests are reviewed, only by the project's own PM, never by
 *       the requester; rejection needs remarks; overrun needs acknowledgement;</li>
 *   <li>delivery is confirmed by a site engineer on the project, only after
 *       payment, for 0 &lt; quantity &lt;= requested; site stock grows.</li>
 * </ul>
 * OOP concept: Abstraction - implements MaterialRequestService.
 */
@Service
public class MaterialRequestServiceImpl implements MaterialRequestService {

    private static final Map<String, String> SORTS = Map.of(
            "createdAt", "createdAt", "requiredBy", "requiredBy", "status", "status", "urgency", "urgency");

    private final MaterialRequestRepository requestRepository;
    private final MaterialRepository materialRepository;
    private final PhaseRepository phaseRepository;
    private final SiteStockRepository stockRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final UserRepository userRepository;
    private final ProjectAccess access;
    private final PhaseBudgetService budgetService;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public MaterialRequestServiceImpl(MaterialRequestRepository requestRepository, MaterialRepository materialRepository,
                                      PhaseRepository phaseRepository, SiteStockRepository stockRepository,
                                      PurchaseOrderRepository purchaseOrderRepository, UserRepository userRepository,
                                      ProjectAccess access, PhaseBudgetService budgetService,
                                      CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher,
                                      Clock clock) {
        this.requestRepository = requestRepository;
        this.materialRepository = materialRepository;
        this.phaseRepository = phaseRepository;
        this.stockRepository = stockRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.budgetService = budgetService;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MaterialRequestResponse> search(Long projectId, RequestStatus status, boolean mineOnly,
                                                        PageQuery pageQuery) {
        if (projectId != null) {
            access.requireVisible(projectId);
        }
        Long me = currentUser.requireId();
        String search = pageQuery.getSearch();
        Specification<MaterialRequest> spec = Specification.where(access.<MaterialRequest>visibleThrough("project"));
        spec = spec.and((root, q, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId))
                .and((root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status))
                .and((root, q, cb) -> mineOnly ? cb.equal(root.get("requestedBy").get("id"), me) : null)
                .and((root, q, cb) -> {
                    if (search == null) {
                        return null;
                    }
                    Join<Object, Object> material = root.join("material");
                    return cb.like(cb.lower(material.get("name")), "%" + search.toLowerCase(Locale.ROOT) + "%");
                });
        return PageResponse.from(requestRepository.findAll(spec, pageQuery.toPageable(SORTS, "createdAt", Sort.Direction.DESC)),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public MaterialRequestResponse get(Long id) {
        MaterialRequest request = find(id);
        access.requireVisible(request.getProject().getId());
        return toResponse(request);
    }

    @Override
    @Transactional
    public MaterialRequestResponse create(Long projectId, MaterialRequestCreate body) {
        Project project = access.requireParticipant(projectId);
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        if (me.getRole() != RoleName.SITE_ENGINEER || !access.isMember(project)) {
            throw new UnauthorizedActionException("materialRequest.create.denied");
        }
        if (body.getRequiredBy().isBefore(LocalDate.now(clock))) {
            throw BusinessRuleException.forField("requiredBy", "materialRequest.requiredBy.past");
        }
        Phase phase = phaseRepository.findById(body.getPhaseId())
                .filter(p -> p.getProject().getId().equals(projectId))
                .orElseThrow(() -> BusinessRuleException.forField("phaseId", "phase.otherProject"));
        if (phase.getStatus() == PhaseStatus.COMPLETED) {
            throw BusinessRuleException.forField("phaseId", "task.phase.completed");
        }
        Material material = materialRepository.findById(body.getMaterialId())
                .filter(Material::isActive)
                .orElseThrow(() -> BusinessRuleException.forField("materialId", "material.unknown"));

        MaterialRequest request = new MaterialRequest(project, phase, material, body.getQuantity(), body.getUrgency(),
                body.getRequiredBy(), body.getNotes(), currentUser.requireUser());
        requestRepository.save(request);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.MATERIAL_REQUEST_CREATED)
                .entityId(request.getId()).args(material.getName(), request.getQuantity(), project.getName())
                .recipients(project.getManager().getId()).link("material-requests/" + request.getId()).build());
        return toResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetCheckResponse budgetCheck(Long id) {
        MaterialRequest request = find(id);
        access.requireVisible(request.getProject().getId());
        return budgetService.check(request.getPhase(), request.getEstimatedCost(), request.getId());
    }

    @Override
    @Transactional
    public MaterialRequestResponse approve(Long id, String remarks, boolean acknowledgeOverrun) {
        MaterialRequest request = requireReviewable(id);
        BudgetCheckResponse budget = budgetService.check(request.getPhase(), request.getEstimatedCost(), request.getId());
        request.prepareBudgetCheck(budget.remaining(), acknowledgeOverrun);
        request.approve(currentUser.requireUser(), remarks);
        request.recordDecision(LocalDateTime.now(clock));

        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.MATERIAL_REQUEST_APPROVED)
                .entityId(id).args(request.getMaterial().getName(), request.getQuantity(), request.getProject().getName())
                .recipients(request.getRequestedBy().getId())
                .recipients(userRepository.findActiveIdsByRole(RoleName.ACCOUNTANT))
                .link("material-requests/" + id).build());
        return toResponse(request);
    }

    @Override
    @Transactional
    public MaterialRequestResponse reject(Long id, String remarks) {
        MaterialRequest request = requireReviewable(id);
        request.reject(currentUser.requireUser(), remarks);
        request.recordDecision(LocalDateTime.now(clock));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.MATERIAL_REQUEST_REJECTED)
                .entityId(id).args(request.getMaterial().getName(), request.getProject().getName(), request.getRemarks())
                .recipients(request.getRequestedBy().getId()).link("material-requests/" + id).build());
        return toResponse(request);
    }

    @Override
    @Transactional
    public MaterialRequestResponse confirmDelivery(Long id, DeliveryRequest body) {
        MaterialRequest request = find(id);
        Project project = access.requireParticipant(request.getProject().getId());
        if (!canConfirmDelivery(currentUser.requirePrincipal(), project)) {
            throw new UnauthorizedActionException("materialRequest.delivery.denied");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        request.confirmDelivery(body.getDeliveredQuantity(), currentUser.requireUser(), body.getNotes(), now);
        SiteStock stock = stockRepository.findByProjectIdAndMaterialId(project.getId(), request.getMaterial().getId())
                .orElseGet(() -> stockRepository.save(new SiteStock(project, request.getMaterial())));
        stock.receive(body.getDeliveredQuantity());
        activeOrder(request).ifPresent(po -> po.markDelivered(now));

        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.MATERIAL_DELIVERED)
                .entityId(id).args(request.getMaterial().getName(), body.getDeliveredQuantity(), project.getName())
                .recipients(project.getManager().getId())
                .recipients(userRepository.findActiveIdsByRole(RoleName.ACCOUNTANT))
                .link("material-requests/" + id).build());
        return toResponse(request);
    }

    // ----- helpers -------------------------------------------------------------------

    /** Only the project's own PM reviews (not the Admin), and never their own request. */
    private MaterialRequest requireReviewable(Long id) {
        MaterialRequest request = find(id);
        Project project = access.requireParticipant(request.getProject().getId());
        if (!project.isManagedBy(currentUser.requireId())) {
            throw new UnauthorizedActionException("materialRequest.review.denied");
        }
        return request;
    }

    private boolean canConfirmDelivery(SiteMasterUserPrincipal me, Project project) {
        return me.getRole() == RoleName.SITE_ENGINEER && access.isMember(project);
    }

    private Optional<PurchaseOrder> activeOrder(MaterialRequest request) {
        return purchaseOrderRepository.findActiveForRequest(request.getId());
    }

    private MaterialRequest find(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Material request"));
    }

    private MaterialRequestResponse toResponse(MaterialRequest r) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        Optional<PurchaseOrder> po = activeOrder(r);
        User approver = r.getApprovedBy();
        boolean canReview = r.getStatus() == RequestStatus.PENDING && r.getProject().isManagedBy(me.getUserId())
                && !r.getRequestedBy().getId().equals(me.getUserId());
        boolean canDeliver = r.getStatus() == RequestStatus.PAID && me.getRole() == RoleName.SITE_ENGINEER
                && access.isMember(r.getProject());
        return new MaterialRequestResponse(r.getId(), r.getProject().getId(), r.getProject().getName(),
                r.getPhase().getId(), r.getPhase().getName(), r.getMaterial().getId(), r.getMaterial().getName(),
                r.getMaterial().getUnit(), r.getQuantity(), r.getUrgency().name(), r.getRequiredBy(),
                r.getEstimatedCost(), r.getNotes(), r.getStatus().name(), r.getRequestedBy().getId(),
                r.getRequestedBy().getFullName(), approver == null ? null : approver.getFullName(), r.getRemarks(),
                r.getDecidedAt(), r.getDeliveredQuantity(), r.getDeliveredAt(),
                r.getDeliveredBy() == null ? null : r.getDeliveredBy().getFullName(), r.getDeliveryNotes(),
                po.map(PurchaseOrder::getId).orElse(null), po.map(PurchaseOrder::getPoNumber).orElse(null),
                po.map(p -> p.getStatus().name()).orElse(null), canReview, canDeliver, r.getCreatedAt());
    }
}
