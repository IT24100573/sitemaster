package com.sitemaster.user;

import com.sitemaster.auth.AuthService;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.role.Role;
import com.sitemaster.role.RoleRepository;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.security.TokenHasher;
import com.sitemaster.user.dto.UserCreateRequest;
import com.sitemaster.user.dto.UserResponse;
import com.sitemaster.user.dto.UserUpdateRequest;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link UserService}. Business rules:
 * <ul>
 *   <li>email unique (case-insensitive);</li>
 *   <li>SUPPLIER accounts are only created by approving a supplier application;</li>
 *   <li>an admin cannot deactivate their own account or change their own role;</li>
 *   <li>the system always keeps at least one active ADMIN.</li>
 * </ul>
 * New accounts get a random unusable password and a set-password link.
 * <p>
 * OOP concept: Abstraction - implements UserService.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Map<String, String> SORTS = Map.of(
            "fullName", "fullName",
            "email", "email",
            "createdAt", "createdAt",
            "lastLoginAt", "lastLoginAt");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder, AuthService authService,
                           CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher, Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String search, RoleName role, Boolean active, PageQuery pageQuery) {
        Specification<User> spec = Specification.where(UserSpecifications.text(pageQuery.getSearch()))
                .and(UserSpecifications.role(role))
                .and(UserSpecifications.active(active));
        LocalDateTime now = now();
        return PageResponse.from(
                userRepository.findAll(spec, pageQuery.toPageable(SORTS, "fullName", Sort.Direction.ASC)),
                user -> UserMapper.toResponse(user, now));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return UserMapper.toResponse(find(id), now());
    }

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (request.getRole() == RoleName.SUPPLIER) {
            throw BusinessRuleException.forField("role", "user.role.supplierViaApplication");
        }
        User user = newAccount(request.getFullName(), request.getEmail(), request.getPhone(), request.getRole());
        UserResponse response = UserMapper.toResponse(user, now());
        publish(WorkflowEventType.USER_CREATED, user, user.getFullName(), user.getRole().getDisplayName());
        return response;
    }

    @Override
    @Transactional
    public User createWorkerAccount(String fullName, String email, String phone) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw DuplicateRecordException.forField("email", "email.duplicate");
        }
        User user = newAccount(fullName, email, phone, RoleName.WORKER);
        publish(WorkflowEventType.USER_CREATED, user, user.getFullName(), user.getRole().getDisplayName());
        return user;
    }

    @Override
    @Transactional
    public User createSupplierAccount(String fullName, String email, String phone) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw DuplicateRecordException.forField("email", "email.duplicate");
        }
        User user = newAccount(fullName, email, phone, RoleName.SUPPLIER);
        publish(WorkflowEventType.USER_CREATED, user, user.getFullName(), user.getRole().getDisplayName());
        return user;
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = find(id);
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(request.getEmail(), id)) {
            throw DuplicateRecordException.forField("email", "email.duplicate");
        }
        if (request.getRole() == RoleName.SUPPLIER && user.getRole().getName() != RoleName.SUPPLIER) {
            throw BusinessRuleException.forField("role", "user.role.supplierViaApplication");
        }
        if (user.getRole().getName() == RoleName.SUPPLIER && request.getRole() != RoleName.SUPPLIER) {
            throw BusinessRuleException.forField("role", "user.role.supplierFixed");
        }

        boolean roleChanged = user.getRole().getName() != request.getRole();
        if (roleChanged) {
            if (isCurrentUser(user)) {
                throw BusinessRuleException.forField("role", "user.role.self");
            }
            ensureAnotherActiveAdminRemains(user);
            user.changeRole(findRole(request.getRole()));
        }
        user.setEmail(request.getEmail());
        user.updateDetails(request.getFullName(), request.getPhone());

        publish(WorkflowEventType.USER_UPDATED, user, user.getFullName());
        if (roleChanged) {
            publish(WorkflowEventType.USER_ROLE_CHANGED, user, user.getFullName(), user.getRole().getDisplayName());
        }
        return UserMapper.toResponse(user, now());
    }

    @Override
    @Transactional
    public UserResponse changeStatus(Long id, boolean active) {
        User user = find(id);
        if (active) {
            user.activate();
            publish(WorkflowEventType.USER_ACTIVATED, user, user.getFullName());
        } else {
            if (isCurrentUser(user)) {
                throw new BusinessRuleException("user.deactivate.self");
            }
            ensureAnotherActiveAdminRemains(user);
            user.deactivate();
            publish(WorkflowEventType.USER_DEACTIVATED, user, user.getFullName());
        }
        return UserMapper.toResponse(user, now());
    }

    @Override
    @Transactional
    public UserResponse unlock(Long id) {
        User user = find(id);
        user.unlock();
        publish(WorkflowEventType.USER_UNLOCKED, user, user.getFullName());
        return UserMapper.toResponse(user, now());
    }

    @Override
    @Transactional
    public void sendPasswordLink(Long id) {
        User user = find(id);
        if (!user.isActive()) {
            throw new BusinessRuleException("user.passwordLink.inactive");
        }
        authService.issuePasswordResetLink(user.getId());
        publish(WorkflowEventType.PASSWORD_LINK_SENT, user, user.getFullName());
    }

    private User newAccount(String fullName, String email, String phone, RoleName roleName) {
        // Unusable random password until the user follows the set-password link.
        String placeholderHash = passwordEncoder.encode(TokenHasher.newToken());
        User user = new User(fullName, email, placeholderHash, findRole(roleName));
        user.updateDetails(fullName, phone);
        userRepository.save(user);
        authService.issuePasswordResetLink(user.getId());
        return user;
    }

    /** Keeps at least one active admin when an admin is deactivated or loses the admin role. */
    private void ensureAnotherActiveAdminRemains(User user) {
        if (user.getRole().getName() == RoleName.ADMIN && user.isActive()
                && userRepository.countByRoleNameAndActiveTrue(RoleName.ADMIN) <= 1) {
            throw new BusinessRuleException("user.lastAdmin");
        }
    }

    private boolean isCurrentUser(User user) {
        return Objects.equals(user.getId(), currentUser.requireId());
    }

    private void publish(WorkflowEventType type, User subject, Object... args) {
        SiteMasterUserPrincipal actor = currentUser.requirePrincipal();
        eventPublisher.publish(WorkflowEvent.builder(type)
                .actor(actor.getUserId(), actor.getFullName())
                .entityId(subject.getId())
                .args(args)
                .recipients(subject.getId())
                .link("profile")
                .build());
    }

    private User find(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "User"));
    }

    private Role findRole(RoleName name) {
        return roleRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Role"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
