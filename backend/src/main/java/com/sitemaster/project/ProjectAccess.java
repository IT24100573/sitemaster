package com.sitemaster.project;

import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * The single place that decides who may see or change a project. Every
 * project-scoped service calls it with the id from the URL, so changing an id in
 * the browser never reveals or changes another project.
 * <ul>
 *   <li>Admin and Accountant see every project.</li>
 *   <li>PMs, site engineers and workers see only projects they are members of;
 *       anything else is reported as "not found".</li>
 *   <li>Only the Admin or the project's own PM may manage (plan, edit) it.</li>
 * </ul>
 * OOP concept: Encapsulation - access rules are hidden behind three methods.
 */
@Component
public class ProjectAccess {

    private static final Set<RoleName> SEE_ALL = EnumSet.of(RoleName.ADMIN, RoleName.ACCOUNTANT);

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository memberRepository;
    private final CurrentUserProvider currentUser;

    public ProjectAccess(ProjectRepository projectRepository, ProjectMemberRepository memberRepository,
                         CurrentUserProvider currentUser) {
        this.projectRepository = projectRepository;
        this.memberRepository = memberRepository;
        this.currentUser = currentUser;
    }

    /** Project the current user may see, else 404. */
    public Project requireVisible(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Project"));
        if (!canView(currentUser.requirePrincipal(), project)) {
            throw new ResourceNotFoundException("resource.notFound", "Project");
        }
        return project;
    }

    /** Visible, not archived, and the current user is the Admin or the project's PM. */
    public Project requireManageable(Long projectId) {
        Project project = requireVisible(projectId);
        if (!isManager(currentUser.requirePrincipal(), project)) {
            throw new UnauthorizedActionException("project.manage.denied");
        }
        project.ensureEditable();
        return project;
    }

    /** Visible and not archived: for team members acting on their own project. */
    public Project requireParticipant(Long projectId) {
        Project project = requireVisible(projectId);
        project.ensureEditable();
        return project;
    }

    public boolean canView(SiteMasterUserPrincipal principal, Project project) {
        return SEE_ALL.contains(principal.getRole())
                || memberRepository.existsByProjectIdAndUserId(project.getId(), principal.getUserId());
    }

    public boolean isManager(SiteMasterUserPrincipal principal, Project project) {
        return principal.getRole() == RoleName.ADMIN
                || (principal.getRole() == RoleName.PROJECT_MANAGER && project.isManagedBy(principal.getUserId()));
    }

    public boolean isManager(Project project) {
        return isManager(currentUser.requirePrincipal(), project);
    }

    public boolean isMember(Project project) {
        return memberRepository.existsByProjectIdAndUserId(project.getId(), currentUser.requireId());
    }

    /**
     * Filter for lists of records that belong to a project (reports, requests, orders):
     * everything for Admin/Accountant, otherwise only records of the user's projects.
     *
     * @param projectAttribute name of the entity's Project attribute, usually "project"
     */
    public <T> Specification<T> visibleThrough(String projectAttribute) {
        SiteMasterUserPrincipal principal = currentUser.requirePrincipal();
        if (SEE_ALL.contains(principal.getRole())) {
            return null;
        }
        Long userId = principal.getUserId();
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ProjectMember> member = sub.from(ProjectMember.class);
            sub.select(member.get("project").get("id")).where(cb.equal(member.get("user").get("id"), userId));
            return root.get(projectAttribute).get("id").in(sub);
        };
    }

    /** Filter for project lists: everything for Admin/Accountant, else own memberships. */
    public Specification<Project> visibleProjects() {
        SiteMasterUserPrincipal principal = currentUser.requirePrincipal();
        if (SEE_ALL.contains(principal.getRole())) {
            return null;
        }
        Long userId = principal.getUserId();
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<ProjectMember> member = sub.from(ProjectMember.class);
            sub.select(member.get("project").get("id")).where(cb.equal(member.get("user").get("id"), userId));
            return root.get("id").in(sub);
        };
    }
}
