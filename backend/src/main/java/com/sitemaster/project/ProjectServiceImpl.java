package com.sitemaster.project;

import com.sitemaster.client.Client;
import com.sitemaster.client.ClientRepository;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.IssueStatus;
import com.sitemaster.common.enums.ProjectStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.common.util.EnumParser;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileDownload;
import com.sitemaster.file.FileStorageService;
import com.sitemaster.file.StoredFile;
import com.sitemaster.issue.IssueRepository;
import com.sitemaster.project.dto.ProjectDetail;
import com.sitemaster.project.dto.ProjectMapMarker;
import com.sitemaster.project.dto.ProjectRequest;
import com.sitemaster.project.dto.ProjectSummary;
import com.sitemaster.project.dto.PublicProjectCard;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.task.TaskRepository;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import jakarta.persistence.criteria.Join;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link ProjectService}. Business rules:
 * <ul>
 *   <li>name unique among non-archived projects; client and PM required (PM must be an
 *       active Project Manager); budget &gt; 0; end date after start date;</li>
 *   <li>only the Admin creates projects, archives them and changes client or PM;</li>
 *   <li>budget cannot drop below the phase budgets, and new dates must still contain
 *       every phase;</li>
 *   <li>archiving is refused while any {@link ProjectArchiveCheck} reports a reason.</li>
 * </ul>
 * OOP concepts: Abstraction (implements ProjectService) and Polymorphism (asks every
 * ProjectArchiveCheck implementation through the same interface).
 */
@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Map<String, String> SORTS = Map.of(
            "name", "name", "startDate", "startDate", "endDate", "endDate",
            "status", "status", "progress", "progress", "createdAt", "createdAt");
    private static final EnumSet<ProjectStatus> MAP_STATUSES =
            EnumSet.of(ProjectStatus.ACTIVE, ProjectStatus.DELAYED, ProjectStatus.COMPLETED);
    private static final EnumSet<IssueStatus> OPEN_ISSUES = EnumSet.of(IssueStatus.OPEN, IssueStatus.IN_PROGRESS);

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final PhaseRepository phaseRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final IssueRepository issueRepository;
    private final FileStorageService fileStorage;
    private final ProjectAccess access;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final ObjectProvider<ProjectArchiveCheck> archiveChecks;
    private final Clock clock;

    public ProjectServiceImpl(ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
                              PhaseRepository phaseRepository, ClientRepository clientRepository,
                              UserRepository userRepository, TaskRepository taskRepository,
                              IssueRepository issueRepository, FileStorageService fileStorage, ProjectAccess access,
                              CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher,
                              ObjectProvider<ProjectArchiveCheck> archiveChecks, Clock clock) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.phaseRepository = phaseRepository;
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.issueRepository = issueRepository;
        this.fileStorage = fileStorage;
        this.access = access;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.archiveChecks = archiveChecks;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProjectSummary> search(ProjectStatus status, PageQuery pageQuery) {
        String search = pageQuery.getSearch();
        Specification<Project> spec = Specification.where(access.visibleProjects())
                .and((root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status))
                .and((root, query, cb) -> {
                    if (search == null) {
                        return null;
                    }
                    Join<Object, Object> client = root.join("client");
                    String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                    return cb.or(cb.like(cb.lower(root.get("name")), like),
                            cb.like(cb.lower(root.get("location")), like),
                            cb.like(cb.lower(client.get("name")), like));
                });
        return PageResponse.from(
                projectRepository.findAll(spec, pageQuery.toPageable(SORTS, "name", Sort.Direction.ASC)),
                ProjectServiceImpl::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectDetail get(Long id) {
        return toDetail(access.requireVisible(id));
    }

    @Override
    @Transactional
    public ProjectDetail create(ProjectRequest request) {
        requireAdmin();
        if (projectRepository.existsOpenWithName(request.getName(), null)) {
            throw DuplicateRecordException.forField("name", "project.name.duplicate");
        }
        User manager = requireProjectManager(request.getManagerId());
        Project project = new Project(request.getName(), requireActiveClient(request.getClientId()), manager);
        project.schedule(request.getStartDate(), request.getEndDate());
        project.updateDetails(request.getName(), request.getDescription(), request.getLocation(),
                request.getLatitude(), request.getLongitude(), request.getBudget());
        projectRepository.save(project);
        memberRepository.save(new ProjectMember(project, manager));

        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PROJECT_CREATED)
                .entityId(project.getId()).args(project.getName(), manager.getFullName())
                .recipients(manager.getId()).link("projects/" + project.getId()).build());
        return toDetail(project);
    }

    @Override
    @Transactional
    public ProjectDetail update(Long id, ProjectRequest request) {
        Project project = access.requireManageable(id);
        boolean admin = isAdmin();
        if (projectRepository.existsOpenWithName(request.getName(), id)) {
            throw DuplicateRecordException.forField("name", "project.name.duplicate");
        }
        if (!admin && (!project.getClient().getId().equals(request.getClientId())
                || !project.getManager().getId().equals(request.getManagerId()))) {
            throw new UnauthorizedActionException("project.adminOnlyFields");
        }
        BigDecimal allocated = phaseRepository.sumBudget(id, null);
        if (request.getBudget().compareTo(allocated) < 0) {
            throw BusinessRuleException.forField("budget", "project.budget.belowAllocated", allocated);
        }
        boolean phasesFit = phaseRepository.findByProjectIdOrderBySortOrderAscStartDateAsc(id).stream()
                .allMatch(p -> !p.getStartDate().isBefore(request.getStartDate())
                        && !p.getEndDate().isAfter(request.getEndDate()));
        if (!phasesFit) {
            throw BusinessRuleException.forField("startDate", "project.dates.phasesOutside");
        }

        project.schedule(request.getStartDate(), request.getEndDate());
        project.updateDetails(request.getName(), request.getDescription(), request.getLocation(),
                request.getLatitude(), request.getLongitude(), request.getBudget());

        if (admin && !project.getClient().getId().equals(request.getClientId())) {
            project.changeClient(requireActiveClient(request.getClientId()));
        }
        if (admin && !project.getManager().getId().equals(request.getManagerId())) {
            User manager = requireProjectManager(request.getManagerId());
            project.changeManager(manager);
            if (!memberRepository.existsByProjectIdAndUserId(id, manager.getId())) {
                memberRepository.save(new ProjectMember(project, manager));
            }
            eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PROJECT_MANAGER_ASSIGNED)
                    .entityId(id).args(project.getName(), manager.getFullName())
                    .recipients(manager.getId()).link("projects/" + id).build());
        }
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PROJECT_UPDATED)
                .entityId(id).args(project.getName()).build());
        return toDetail(project);
    }

    @Override
    @Transactional
    public ProjectDetail changeStatus(Long id, String status) {
        ProjectStatus target = EnumParser.parse(ProjectStatus.class, status, "status");
        Project project = access.requireManageable(id);
        if (target == ProjectStatus.ARCHIVED) {
            requireAdmin();
            archiveChecks.orderedStream()
                    .map(check -> check.blockingReason(id))
                    .flatMap(Optional::stream)
                    .findFirst()
                    .ifPresent(reason -> {
                        throw new BusinessRuleException(reason);
                    });
        }
        ProjectStatus previous = project.getStatus();
        project.changeStatus(target, LocalDate.now(clock));

        WorkflowEvent.Builder event = WorkflowEvent.byCurrentUser(WorkflowEventType.PROJECT_STATUS_CHANGED)
                .entityId(id).args(project.getName(), previous, target).link("projects/" + id);
        if (!project.isManagedBy(currentUser.requireId())) {
            event.recipients(project.getManager().getId());
        }
        eventPublisher.publish(event.build());
        return toDetail(project);
    }

    @Override
    @Transactional
    public ProjectDetail changeCover(Long id, MultipartFile file) {
        Project project = access.requireManageable(id);
        StoredFile previous = project.getCoverPhoto();
        project.changeCoverPhoto(fileStorage.store(file, FileCategory.IMAGE, "file"));
        projectRepository.saveAndFlush(project);
        if (previous != null) {
            fileStorage.delete(previous);
        }
        return toDetail(project);
    }

    @Override
    @Transactional
    public ProjectDetail removeCover(Long id) {
        Project project = access.requireManageable(id);
        StoredFile previous = project.getCoverPhoto();
        if (previous != null) {
            project.changeCoverPhoto(null);
            projectRepository.saveAndFlush(project);
            fileStorage.delete(previous);
        }
        return toDetail(project);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload openCover(Long id) {
        Project project = access.requireVisible(id);
        if (project.getCoverPhoto() == null) {
            throw new ResourceNotFoundException("resource.notFound", "Cover photo");
        }
        return fileStorage.open(project.getCoverPhoto());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicProjectCard> completedGallery() {
        return projectRepository.findByStatusOrderByCompletedDateDesc(ProjectStatus.COMPLETED).stream()
                .map(p -> new PublicProjectCard(p.getId(), p.getName(), p.getClient().getName(), p.getLocation(),
                        p.getCompletedDate(), p.hasCoverPhoto()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectMapMarker> mapMarkers() {
        return projectRepository.findByStatusInOrderByNameAsc(MAP_STATUSES).stream()
                .map(p -> new ProjectMapMarker(p.getId(), p.getName(), p.getStatus().name(), p.getProgress(),
                        p.getLocation(), p.getLatitude(), p.getLongitude()))
                .toList();
    }

    /** Only completed projects' covers are public. */
    @Override
    @Transactional(readOnly = true)
    public FileDownload openPublicCover(Long id) {
        Project project = projectRepository.findById(id)
                .filter(p -> p.getStatus() == ProjectStatus.COMPLETED && p.getCoverPhoto() != null)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Cover photo"));
        return fileStorage.open(project.getCoverPhoto());
    }

    // ----- helpers ---------------------------------------------------------------

    private Client requireActiveClient(Long clientId) {
        return clientRepository.findById(clientId)
                .filter(Client::isActive)
                .orElseThrow(() -> BusinessRuleException.forField("clientId", "project.client.invalid"));
    }

    private User requireProjectManager(Long userId) {
        return userRepository.findById(userId)
                .filter(u -> u.isActive() && u.getRole().getName() == RoleName.PROJECT_MANAGER)
                .orElseThrow(() -> BusinessRuleException.forField("managerId", "project.manager.invalid"));
    }

    private boolean isAdmin() {
        return currentUser.requirePrincipal().getRole() == RoleName.ADMIN;
    }

    private void requireAdmin() {
        if (!isAdmin()) {
            throw new UnauthorizedActionException("project.adminOnly");
        }
    }

    private static ProjectSummary toSummary(Project p) {
        return new ProjectSummary(p.getId(), p.getName(), p.getClient().getName(), p.getManager().getFullName(),
                p.getLocation(), p.getStatus().name(), p.getProgress(), p.getStartDate(), p.getEndDate(),
                p.getBudget(), p.hasCoverPhoto());
    }

    private ProjectDetail toDetail(Project p) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        boolean canManage = access.isManager(me, p) && p.getStatus() != ProjectStatus.ARCHIVED;
        List<String> next = canManage
                ? p.nextStatuses().stream()
                        .filter(s -> s != ProjectStatus.ARCHIVED || me.getRole() == RoleName.ADMIN)
                        .map(Enum::name).toList()
                : List.of();
        return new ProjectDetail(p.getId(), p.getName(), p.getDescription(), p.getClient().getId(),
                p.getClient().getName(), p.getManager().getId(), p.getManager().getFullName(), p.getLocation(),
                p.getLatitude(), p.getLongitude(), p.getStartDate(), p.getEndDate(), p.getCompletedDate(),
                p.getBudget(), phaseRepository.sumBudget(p.getId(), null), p.getStatus().name(), next,
                p.getProgress(), p.hasCoverPhoto(), memberRepository.findUserIds(p.getId()).size(),
                taskRepository.countByProject(p.getId()),
                issueRepository.countByProjectIdAndStatusIn(p.getId(), OPEN_ISSUES),
                canManage, access.isMember(p));
    }
}
