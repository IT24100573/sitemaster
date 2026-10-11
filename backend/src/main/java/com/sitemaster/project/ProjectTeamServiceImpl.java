package com.sitemaster.project;

import com.sitemaster.common.dto.LookupOption;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.enums.TaskStatus;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.project.dto.MemberResponse;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.task.SubTaskRepository;
import com.sitemaster.task.TaskRepository;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link ProjectTeamService}. Rules:
 * <ul>
 *   <li>Admin or the project's PM may add/remove site engineers and workers;</li>
 *   <li>a site engineer who is on the project may add/remove workers only;</li>
 *   <li>the PM cannot be removed, and nobody with open tasks or sub-tasks on the
 *       project can be removed.</li>
 * </ul>
 * OOP concept: Abstraction - implements ProjectTeamService.
 */
@Service
public class ProjectTeamServiceImpl implements ProjectTeamService {

    private static final Set<RoleName> MANAGER_CAN_ADD = EnumSet.of(RoleName.SITE_ENGINEER, RoleName.WORKER);
    private static final Set<RoleName> ENGINEER_CAN_ADD = EnumSet.of(RoleName.WORKER);

    private final ProjectAccess access;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;

    public ProjectTeamServiceImpl(ProjectAccess access, ProjectMemberRepository memberRepository,
                                  UserRepository userRepository, TaskRepository taskRepository,
                                  SubTaskRepository subTaskRepository, CurrentUserProvider currentUser,
                                  WorkflowEventPublisher eventPublisher) {
        this.access = access;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.subTaskRepository = subTaskRepository;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberResponse> members(Long projectId) {
        Project project = access.requireVisible(projectId);
        return memberRepository.findWithUsers(projectId).stream().map(m -> toResponse(m, project)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupOption> candidates(Long projectId) {
        Project project = access.requireParticipant(projectId);
        Set<RoleName> allowed = assignableRoles(project);
        Set<Long> existing = new HashSet<>(memberRepository.findUserIds(projectId));
        return userRepository.findAll(Sort.by("fullName")).stream()
                .filter(u -> u.isActive() && allowed.contains(u.getRole().getName()) && !existing.contains(u.getId()))
                .map(u -> new LookupOption(String.valueOf(u.getId()),
                        u.getFullName() + " (" + u.getRole().getDisplayName() + ")"))
                .toList();
    }

    @Override
    @Transactional
    public MemberResponse add(Long projectId, Long userId) {
        Project project = access.requireParticipant(projectId);
        Set<RoleName> allowed = assignableRoles(project);
        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> BusinessRuleException.forField("userId", "project.member.invalidUser"));
        if (!allowed.contains(user.getRole().getName())) {
            throw BusinessRuleException.forField("userId", "project.member.roleNotAllowed");
        }
        if (memberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw DuplicateRecordException.forField("userId", "project.member.duplicate");
        }
        ProjectMember member = memberRepository.save(new ProjectMember(project, user));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PROJECT_MEMBER_ADDED)
                .entityId(projectId).args(user.getFullName(), project.getName())
                .recipients(user.getId()).link("projects/" + projectId).build());
        return toResponse(member, project);
    }

    @Override
    @Transactional
    public void remove(Long projectId, Long userId) {
        Project project = access.requireParticipant(projectId);
        Set<RoleName> allowed = assignableRoles(project);
        ProjectMember member = memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Team member"));
        User user = member.getUser();
        if (project.isManagedBy(userId)) {
            throw new BusinessRuleException("project.member.removeManager");
        }
        if (!allowed.contains(user.getRole().getName())) {
            throw new UnauthorizedActionException("project.member.removeDenied");
        }
        long openWork = taskRepository.countByProjectIdAndAssigneeIdAndStatusNot(projectId, userId, TaskStatus.DONE)
                + subTaskRepository.countOpenForUser(projectId, userId, TaskStatus.DONE);
        if (openWork > 0) {
            throw new BusinessRuleException("project.member.openWork", openWork);
        }
        memberRepository.delete(member);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PROJECT_MEMBER_REMOVED)
                .entityId(projectId).args(user.getFullName(), project.getName())
                .recipients(user.getId()).build());
    }

    /** Which roles the current user may add to or remove from this project. */
    private Set<RoleName> assignableRoles(Project project) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        if (access.isManager(me, project)) {
            return MANAGER_CAN_ADD;
        }
        if (me.getRole() == RoleName.SITE_ENGINEER && access.isMember(project)) {
            return ENGINEER_CAN_ADD;
        }
        throw new UnauthorizedActionException("project.member.manageDenied");
    }

    private static MemberResponse toResponse(ProjectMember m, Project project) {
        User u = m.getUser();
        return new MemberResponse(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getRole().getName().name(),
                u.getRole().getDisplayName(), u.hasProfilePhoto(), project.isManagedBy(u.getId()), m.getCreatedAt());
    }
}
