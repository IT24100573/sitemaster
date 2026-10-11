package com.sitemaster.supplier;

import com.sitemaster.common.dto.MessageResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.ApplicationStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.util.MessageResolver;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileDownload;
import com.sitemaster.file.FileStorageService;
import com.sitemaster.file.StoredFile;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.settings.SystemSettingsManager;
import com.sitemaster.supplier.dto.SupplierApplicationForm;
import com.sitemaster.supplier.dto.SupplierApplicationResponse;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import com.sitemaster.user.UserService;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link SupplierApplicationService}. Rules:
 * <ul>
 *   <li>no second PENDING application with the same email or registration number;</li>
 *   <li>categories must come from the list in System Settings;</li>
 *   <li>on approval: email must not belong to an existing account and the
 *       registration number must not belong to an existing supplier; a Supplier
 *       and a SUPPLIER user are created and a set-password link is issued;</li>
 *   <li>submission notifies every active Admin; approval notifies the new supplier.</li>
 * </ul>
 * OOP concept: Abstraction - implements SupplierApplicationService.
 */
@Service
public class SupplierApplicationServiceImpl implements SupplierApplicationService {

    private static final Logger log = LoggerFactory.getLogger(SupplierApplicationServiceImpl.class);
    private static final Map<String, String> SORTS = Map.of("createdAt", "createdAt", "companyName", "companyName");

    private final SupplierApplicationRepository applicationRepository;
    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final FileStorageService fileStorage;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final MessageResolver messages;
    private final Clock clock;

    public SupplierApplicationServiceImpl(SupplierApplicationRepository applicationRepository,
                                          SupplierRepository supplierRepository, UserRepository userRepository,
                                          UserService userService, FileStorageService fileStorage,
                                          CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher,
                                          MessageResolver messages, Clock clock) {
        this.applicationRepository = applicationRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.fileStorage = fileStorage;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.messages = messages;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MessageResponse submit(SupplierApplicationForm form) {
        if (applicationRepository.existsByEmailIgnoreCaseAndStatus(form.getEmail(), ApplicationStatus.PENDING)) {
            throw DuplicateRecordException.forField("email", "supplierApplication.pendingEmail");
        }
        if (applicationRepository.existsByRegistrationNoIgnoreCaseAndStatus(form.getRegistrationNo(),
                ApplicationStatus.PENDING)) {
            throw DuplicateRecordException.forField("registrationNo", "supplierApplication.pendingRegistration");
        }
        List<String> categories = validCategories(form.getMaterialCategories());
        StoredFile document = fileStorage.store(form.getRegistrationDocument(), FileCategory.DOCUMENT,
                "registrationDocument");
        SupplierApplication application = new SupplierApplication(form.getCompanyName(), form.getRegistrationNo(),
                form.getContactPerson(), form.getEmail(), form.getPhone(), form.getAddress(), categories, document);
        applicationRepository.save(application);

        eventPublisher.publish(WorkflowEvent.builder(WorkflowEventType.SUPPLIER_APPLICATION_SUBMITTED)
                .actor(null, application.getContactPerson())
                .entityId(application.getId())
                .args(application.getCompanyName())
                .recipients(userRepository.findActiveIdsByRole(RoleName.ADMIN))
                .link("supplier-applications/" + application.getId())
                .build());
        return new MessageResponse(messages.get("supplierApplication.submitted"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SupplierApplicationResponse> search(ApplicationStatus status, PageQuery pageQuery) {
        String search = pageQuery.getSearch();
        Specification<SupplierApplication> spec = (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
        spec = spec.and((root, query, cb) -> {
            if (search == null) {
                return null;
            }
            String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
            return cb.or(cb.like(cb.lower(root.get("companyName")), like), cb.like(cb.lower(root.get("email")), like),
                    cb.like(cb.lower(root.get("registrationNo")), like));
        });
        return PageResponse.from(
                applicationRepository.findAll(spec, pageQuery.toPageable(SORTS, "createdAt", Sort.Direction.DESC)),
                SupplierMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierApplicationResponse get(Long id) {
        return SupplierMapper.toResponse(find(id));
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload openDocument(Long id) {
        return fileStorage.open(find(id).getRegistrationDocument());
    }

    @Override
    @Transactional
    public SupplierApplicationResponse approve(Long id, String remarks) {
        SupplierApplication application = find(id);
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BusinessRuleException("supplierApplication.notPending");
        }
        if (userRepository.existsByEmailIgnoreCase(application.getEmail())) {
            throw new DuplicateRecordException("supplierApplication.emailInUse");
        }
        if (supplierRepository.existsByRegistrationNoIgnoreCase(application.getRegistrationNo())) {
            throw new DuplicateRecordException("supplierApplication.registrationInUse");
        }
        User account = userService.createSupplierAccount(application.getContactPerson(), application.getEmail(),
                application.getPhone());
        Supplier supplier = new Supplier(account, application.getCompanyName(), application.getRegistrationNo());
        supplier.updateContact(application.getContactPerson(), application.getEmail(), application.getPhone(),
                application.getAddress(), application.getMaterialCategories());
        supplierRepository.save(supplier);
        application.approve(currentUser.requireUser(), supplier, remarks, LocalDateTime.now(clock));

        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.SUPPLIER_APPLICATION_APPROVED)
                .entityId(application.getId()).args(application.getCompanyName())
                .recipients(account.getId()).link("dashboard").build());
        return SupplierMapper.toResponse(application);
    }

    @Override
    @Transactional
    public SupplierApplicationResponse reject(Long id, String remarks) {
        SupplierApplication application = find(id);
        application.reject(currentUser.requireUser(), remarks, LocalDateTime.now(clock));
        // No account exists yet, so the applicant is informed outside the app.
        log.info("Supplier application {} from {} rejected; inform the applicant at {}",
                application.getId(), application.getCompanyName(), application.getEmail());
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.SUPPLIER_APPLICATION_REJECTED)
                .entityId(application.getId()).args(application.getCompanyName(), application.getReviewRemarks())
                .build());
        return SupplierMapper.toResponse(application);
    }

    private List<String> validCategories(List<String> requested) {
        List<String> allowed = SystemSettingsManager.getInstance().getSettings().materialCategories();
        LinkedHashSet<String> unique = new LinkedHashSet<>(requested);
        if (!allowed.containsAll(unique)) {
            throw BusinessRuleException.forField("materialCategories", "materialCategories.unknown");
        }
        return List.copyOf(unique);
    }

    private SupplierApplication find(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Supplier application"));
    }
}
