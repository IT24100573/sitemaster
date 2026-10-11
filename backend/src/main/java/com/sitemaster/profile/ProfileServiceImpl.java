package com.sitemaster.profile;

import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.employee.EmployeeRepository;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileDownload;
import com.sitemaster.file.FileStorageService;
import com.sitemaster.file.StoredFile;
import com.sitemaster.profile.dto.ChangePasswordRequest;
import com.sitemaster.profile.dto.ProfileResponse;
import com.sitemaster.profile.dto.ProfileUpdateRequest;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

/**
 * Implementation of {@link ProfileService}. Everything is scoped to the
 * signed-in user; no user id is taken from the client for changes.
 * <p>
 * OOP concept: Abstraction - implements ProfileService.
 */
@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final FileStorageService fileStorage;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;

    public ProfileServiceImpl(UserRepository userRepository, EmployeeRepository employeeRepository,
                              FileStorageService fileStorage, PasswordEncoder passwordEncoder,
                              CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.fileStorage = fileStorage;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getMine() {
        return toResponse(currentUser.requireUser());
    }

    @Override
    @Transactional
    public ProfileResponse updateMine(ProfileUpdateRequest request) {
        User user = currentUser.requireUser();
        user.updateDetails(request.getFullName(), request.getPhone());
        publish(WorkflowEventType.PROFILE_UPDATED, user, false);
        return toResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse changeMyPhoto(MultipartFile file) {
        User user = currentUser.requireUser();
        StoredFile previous = user.getProfilePhoto();
        user.changeProfilePhoto(fileStorage.store(file, FileCategory.IMAGE, "file"));
        userRepository.saveAndFlush(user);
        if (previous != null) {
            fileStorage.delete(previous);
        }
        return toResponse(user);
    }

    @Override
    @Transactional
    public ProfileResponse removeMyPhoto() {
        User user = currentUser.requireUser();
        StoredFile previous = user.getProfilePhoto();
        if (previous != null) {
            user.changeProfilePhoto(null);
            userRepository.saveAndFlush(user);
            fileStorage.delete(previous);
        }
        return toResponse(user);
    }

    @Override
    @Transactional
    public void changeMyPassword(ChangePasswordRequest request) {
        User user = currentUser.requireUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw BusinessRuleException.forField("currentPassword", "profile.password.currentWrong");
        }
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw BusinessRuleException.forField("newPassword", "profile.password.sameAsCurrent");
        }
        user.changePasswordHash(passwordEncoder.encode(request.getNewPassword()));
        publish(WorkflowEventType.PASSWORD_CHANGED, user, true);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload openPhoto(Long userId) {
        SiteMasterUserPrincipal viewer = currentUser.requirePrincipal();
        if (viewer.getRole() == RoleName.SUPPLIER && !Objects.equals(viewer.getUserId(), userId)) {
            throw new ResourceNotFoundException("resource.notFound", "Photo");
        }
        StoredFile photo = userRepository.findById(userId)
                .map(User::getProfilePhoto)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Photo"));
        return fileStorage.open(photo);
    }

    private void publish(WorkflowEventType type, User user, boolean notifyUser) {
        WorkflowEvent.Builder builder = WorkflowEvent.builder(type)
                .actor(user.getId(), user.getFullName())
                .entityId(user.getId())
                .link("profile");
        if (notifyUser) {
            builder.recipients(user.getId());
        }
        eventPublisher.publish(builder.build());
    }

    private ProfileResponse toResponse(User user) {
        ProfileResponse.Employment employment = employeeRepository.findByUserId(user.getId())
                .map(e -> new ProfileResponse.Employment(e.getEmployeeNo(), e.getEmploymentType().name(),
                        e.getTrade(), e.getJoinDate()))
                .orElse(null);
        return new ProfileResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getRole().getName().name(), user.getRole().getDisplayName(), user.hasProfilePhoto(),
                user.getLastLoginAt(), employment);
    }
}
