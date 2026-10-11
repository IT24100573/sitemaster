package com.sitemaster.task;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.PhaseStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.project.Phase;
import com.sitemaster.project.PhaseRepository;
import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.project.ProjectMemberRepository;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.task.dto.MyWorkResponse;
import com.sitemaster.task.dto.SubTaskRequest;
import com.sitemaster.task.dto.SubTaskResponse;
import com.sitemaster.task.dto.TaskProgressRequest;
import com.sitemaster.task.dto.TaskRequest;
import com.sitemaster.task.dto.TaskResponse;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link TaskService}. Who may do what:
 * <ul>
 *   <li>create / edit / delete tasks: the Admin or the project's PM;</li>
 *   <li>update progress: the PM, the assignee, or a site engineer on the project;</li>
 *   <li>sub-tasks: the PM or a site engineer on the project creates them; the
 *       sub-task's assignee, the task's assignee or the PM changes their status;</li>
 *   <li>workers only ever see tasks and sub-tasks assigned to them.</li>
 * </ul>
 * Rules: due date inside the phase, assignee on the project, progress 0-100, DONE = 100%.
 * Project progress is recalculated when the PM approves a daily report, not on each task change.
 * <p>
 * OOP concept: Abstraction - implements TaskService.
 */
@Service
public class TaskServiceImpl implements TaskService {

    private static final Map<String, String> SORTS = Map.of(
            "dueDate", "dueDate", "title", "title", "status", "status", "priority", "priority",
            "progress", "progress", "createdAt", "createdAt");

    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final PhaseRepository phaseRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectAccess access;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public TaskServiceImpl(TaskRepository taskRepository, SubTaskRepository subTaskRepository,
                           PhaseRepository phaseRepository, ProjectMemberRepository memberRepository,
                           UserRepository userRepository, ProjectAccess access,
                           CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher, Clock clock) {
        this.taskRepository = taskRepository;
        this.subTaskRepository = subTaskRepository;
        this.phaseRepository = phaseRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> list(Long projectId, Long phaseId, TaskStatus status, Long assigneeId,
                                           PageQuery pageQuery) {
        access.requireVisible(projectId);
        Long onlyAssignee = isWorker() ? currentUser.requireId() : assigneeId;
        String search = pageQuery.getSearch();
        Specification<Task> spec = (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
        spec = spec.and((root, query, cb) -> phaseId == null ? null : cb.equal(root.get("phase").get("id"), phaseId))
                .and((root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status))
                .and((root, query, cb) -> onlyAssignee == null ? null
                        : cb.equal(root.get("assignee").get("id"), onlyAssignee))
                .and((root, query, cb) -> {
                    if (search == null) {
                        return null;
                    }
                    Join<Object, Object> assignee = root.join("assignee", JoinType.LEFT);
                    String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                    return cb.or(cb.like(cb.lower(root.get("title")), like),
                            cb.like(cb.lower(cb.coalesce(assignee.get("fullName"), "")), like));
                });
        return PageResponse.from(taskRepository.findAll(spec, pageQuery.toPageable(SORTS, "dueDate", Sort.Direction.ASC)),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse get(Long taskId) {
        Task task = findVisibleTask(taskId);
        if (isWorker() && !task.isAssignedTo(currentUser.requireId())
                && task.getSubTasks().stream().noneMatch(s -> s.isAssignedTo(currentUser.requireId()))) {
            throw new ResourceNotFoundException("resource.notFound", "Task");
        }
        return toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse create(Long projectId, TaskRequest request) {
        Project project = access.requireManageable(projectId);
        Task task = new Task(project);
        task.plan(findPhase(project, request.getPhaseId()), request.getTitle(), request.getDescription(),
                request.getPriority(), request.getDueDate());
        User assignee = resolveMember(project, request.getAssigneeId(), "assigneeId");
        task.assignTo(assignee);
        taskRepository.save(task);
        notifyAssigned(task, assignee);
        return toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse update(Long taskId, TaskRequest request) {
        Task task = findTask(taskId);
        Project project = access.requireManageable(task.getProject().getId());
        task.plan(findPhase(project, request.getPhaseId()), request.getTitle(), request.getDescription(),
                request.getPriority(), request.getDueDate());
        User assignee = resolveMember(project, request.getAssigneeId(), "assigneeId");
        boolean reassigned = !Objects.equals(idOf(task.getAssignee()), idOf(assignee));
        task.assignTo(assignee);
        if (reassigned) {
            notifyAssigned(task, assignee);
        } else {
            eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.TASK_UPDATED)
                    .entityId(task.getId()).args(task.getTitle(), project.getName()).build());
        }
        return toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateProgress(Long taskId, TaskProgressRequest request) {
        Task task = findTask(taskId);
        Project project = access.requireParticipant(task.getProject().getId());
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        boolean allowed = access.isManager(me, project) || task.isAssignedTo(me.getUserId())
                || (me.getRole() == RoleName.SITE_ENGINEER && access.isMember(project));
        if (!allowed) {
            throw new UnauthorizedActionException("task.update.denied");
        }
        boolean wasOpen = task.isOpen();
        task.applyProgress(request.getStatus(), request.getProgress(), LocalDateTime.now(clock));
        if (task.getStatus() != TaskStatus.TODO && task.getPhase().getStatus() == PhaseStatus.NOT_STARTED) {
            task.getPhase().start();
        }

        WorkflowEvent.Builder event = WorkflowEvent.byCurrentUser(
                        wasOpen && !task.isOpen() ? WorkflowEventType.TASK_COMPLETED : WorkflowEventType.TASK_UPDATED)
                .entityId(task.getId()).args(task.getTitle(), project.getName())
                .link("projects/" + project.getId());
        if (wasOpen && !task.isOpen() && !project.isManagedBy(me.getUserId())) {
            event.recipients(project.getManager().getId());
        }
        eventPublisher.publish(event.build());
        return toResponse(task);
    }

    @Override
    @Transactional
    public void delete(Long taskId) {
        Task task = findTask(taskId);
        Project project = access.requireManageable(task.getProject().getId());
        String title = task.getTitle();
        taskRepository.delete(task);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.TASK_DELETED)
                .entityId(taskId).args(title, project.getName()).build());
    }

    // ----- sub-tasks -------------------------------------------------------------

    @Override
    @Transactional
    public TaskResponse addSubTask(Long taskId, SubTaskRequest request) {
        Task task = findTask(taskId);
        Project project = requirePlanner(task);
        SubTask subTask = new SubTask(task);
        User assignee = resolveMember(project, request.getAssigneeId(), "assigneeId");
        subTask.update(request.getTitle(), assignee, request.getDueDate());
        task.addSubTask(subTask);
        taskRepository.saveAndFlush(task);
        if (assignee != null) {
            eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.SUBTASK_ASSIGNED)
                    .entityId(subTask.getId()).args(subTask.getTitle(), task.getTitle(), project.getName())
                    .recipients(assignee.getId()).link("my-tasks").build());
        }
        return toResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse updateSubTask(Long subTaskId, SubTaskRequest request) {
        SubTask subTask = findSubTask(subTaskId);
        Task task = subTask.getTask();
        Project project = requirePlanner(task);
        User assignee = resolveMember(project, request.getAssigneeId(), "assigneeId");
        boolean reassigned = assignee != null && !Objects.equals(idOf(subTask.getAssignee()), assignee.getId());
        subTask.update(request.getTitle(), assignee, request.getDueDate());
        if (reassigned) {
            eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.SUBTASK_ASSIGNED)
                    .entityId(subTask.getId()).args(subTask.getTitle(), task.getTitle(), project.getName())
                    .recipients(assignee.getId()).link("my-tasks").build());
        }
        return toResponse(task);
    }

    @Override
    @Transactional
    public SubTaskResponse changeSubTaskStatus(Long subTaskId, TaskStatus status) {
        SubTask subTask = findSubTask(subTaskId);
        Task task = subTask.getTask();
        Project project = access.requireParticipant(task.getProject().getId());
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        boolean allowed = subTask.isAssignedTo(me.getUserId()) || task.isAssignedTo(me.getUserId())
                || access.isManager(me, project)
                || (me.getRole() == RoleName.SITE_ENGINEER && access.isMember(project));
        if (!allowed) {
            throw new UnauthorizedActionException("task.update.denied");
        }
        boolean completedNow = status == TaskStatus.DONE && subTask.getStatus() != TaskStatus.DONE;
        subTask.changeStatus(status, LocalDateTime.now(clock));
        if (completedNow) {
            WorkflowEvent.Builder event = WorkflowEvent.byCurrentUser(WorkflowEventType.SUBTASK_COMPLETED)
                    .entityId(subTask.getId()).args(subTask.getTitle(), task.getTitle(), project.getName())
                    .link("projects/" + project.getId());
            if (task.getAssignee() != null && !task.isAssignedTo(me.getUserId())) {
                event.recipients(task.getAssignee().getId());
            }
            eventPublisher.publish(event.build());
        }
        return toSubResponse(subTask);
    }

    @Override
    @Transactional
    public TaskResponse deleteSubTask(Long subTaskId) {
        SubTask subTask = findSubTask(subTaskId);
        Task task = subTask.getTask();
        Project project = requirePlanner(task);
        String title = subTask.getTitle();
        task.removeSubTask(subTask);
        taskRepository.saveAndFlush(task);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.SUBTASK_DELETED)
                .entityId(subTaskId).args(title, task.getTitle(), project.getName()).build());
        return toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public MyWorkResponse myWork() {
        Long me = currentUser.requireId();
        return new MyWorkResponse(
                taskRepository.findAssignedTo(me).stream().map(this::toResponse).toList(),
                subTaskRepository.findAssignedTo(me).stream().map(this::toSubResponse).toList());
    }

    // ----- helpers -------------------------------------------------------------------

    /** PM/Admin, or a site engineer on the project, may plan sub-tasks. */
    private Project requirePlanner(Task task) {
        Project project = access.requireParticipant(task.getProject().getId());
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        boolean allowed = access.isManager(me, project)
                || (me.getRole() == RoleName.SITE_ENGINEER && access.isMember(project));
        if (!allowed) {
            throw new UnauthorizedActionException("task.subtask.denied");
        }
        return project;
    }

    private User resolveMember(Project project, Long userId, String field) {
        if (userId == null) {
            return null;
        }
        if (!memberRepository.existsByProjectIdAndUserId(project.getId(), userId)) {
            throw BusinessRuleException.forField(field, "task.assignee.notMember");
        }
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> BusinessRuleException.forField(field, "task.assignee.notMember"));
    }

    private Phase findPhase(Project project, Long phaseId) {
        Phase phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> BusinessRuleException.forField("phaseId", "phase.otherProject"));
        if (!phase.getProject().getId().equals(project.getId())) {
            throw BusinessRuleException.forField("phaseId", "phase.otherProject");
        }
        if (phase.getStatus() == PhaseStatus.COMPLETED) {
            throw BusinessRuleException.forField("phaseId", "task.phase.completed");
        }
        return phase;
    }

    private void notifyAssigned(Task task, User assignee) {
        WorkflowEvent.Builder event = WorkflowEvent.byCurrentUser(WorkflowEventType.TASK_ASSIGNED)
                .entityId(task.getId()).args(task.getTitle(), task.getProject().getName(),
                        assignee == null ? "-" : assignee.getFullName())
                .link("my-tasks");
        if (assignee != null) {
            event.recipients(assignee.getId());
        }
        eventPublisher.publish(event.build());
    }

    private Task findVisibleTask(Long taskId) {
        Task task = findTask(taskId);
        access.requireVisible(task.getProject().getId());
        return task;
    }

    private Task findTask(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Task"));
    }

    private SubTask findSubTask(Long id) {
        return subTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Sub-task"));
    }

    private boolean isWorker() {
        return currentUser.requirePrincipal().getRole() == RoleName.WORKER;
    }

    private static Long idOf(User user) {
        return user == null ? null : user.getId();
    }

    private TaskResponse toResponse(Task t) {
        boolean overdue = t.isOpen() && t.getDueDate().isBefore(LocalDate.now(clock));
        return new TaskResponse(t.getId(), t.getProject().getId(), t.getProject().getName(), t.getPhase().getId(),
                t.getPhase().getName(), t.getTitle(), t.getDescription(), idOf(t.getAssignee()),
                t.getAssignee() == null ? null : t.getAssignee().getFullName(), t.getPriority().name(),
                t.getDueDate(), t.getStatus().name(), t.getProgress(), t.getCompletedAt(), overdue,
                t.getSubTasks().stream().map(this::toSubResponse).toList());
    }

    private SubTaskResponse toSubResponse(SubTask s) {
        Task t = s.getTask();
        return new SubTaskResponse(s.getId(), t.getId(), t.getTitle(), t.getProject().getId(), t.getProject().getName(),
                s.getTitle(), idOf(s.getAssignee()), s.getAssignee() == null ? null : s.getAssignee().getFullName(),
                s.getStatus().name(), s.getDueDate(), s.getCompletedAt());
    }
}
