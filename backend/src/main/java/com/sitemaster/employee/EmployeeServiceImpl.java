package com.sitemaster.employee;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.EmploymentType;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.employee.dto.EmployeeCreateRequest;
import com.sitemaster.employee.dto.EmployeeDetailsRequest;
import com.sitemaster.employee.dto.EmployeeListItem;
import com.sitemaster.employee.dto.EmployeeResponse;
import com.sitemaster.employee.dto.EmployeeUpdateRequest;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileDownload;
import com.sitemaster.file.FileStorageService;
import com.sitemaster.file.StoredFile;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.settings.SystemSettingsManager;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import com.sitemaster.user.UserService;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link EmployeeService}. Business rules:
 * <ul>
 *   <li>NIC unique; age 18-65 from date of birth; join date not in the future;</li>
 *   <li>trade required for workers and must be one of the trades in System Settings;</li>
 *   <li>pay rules per employment type (enforced by {@link Employee#applyPayTerms});</li>
 *   <li>HR either creates a new WORKER account or links an existing staff account
 *       that has no employee record yet (never a supplier).</li>
 * </ul>
 * OOP concept: Abstraction - implements EmployeeService.
 */
@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final String EMPLOYEE_NO_FORMAT = "EMP-%04d";
    private static final int MAX_DOCUMENTS = 20;
    private static final Map<String, String> SORTS = Map.of(
            "employeeNo", "employeeNo",
            "fullName", "user.fullName",
            "joinDate", "joinDate",
            "employmentType", "employmentType",
            "createdAt", "createdAt");

    private final EmployeeRepository employeeRepository;
    private final EmployeeDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final FileStorageService fileStorage;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository, EmployeeDocumentRepository documentRepository,
                               UserRepository userRepository, UserService userService, FileStorageService fileStorage,
                               CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher, Clock clock) {
        this.employeeRepository = employeeRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.fileStorage = fileStorage;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EmployeeListItem> search(EmploymentType type, String trade, Boolean active,
                                                 PageQuery pageQuery) {
        Specification<Employee> spec = Specification.where(EmployeeSpecifications.text(pageQuery.getSearch()))
                .and(EmployeeSpecifications.type(type))
                .and(EmployeeSpecifications.trade(trade))
                .and(EmployeeSpecifications.active(active));
        return PageResponse.from(
                employeeRepository.findAll(spec, pageQuery.toPageable(SORTS, "employeeNo", Sort.Direction.ASC)),
                EmployeeMapper::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponse get(Long id) {
        return EmployeeMapper.toResponse(find(id), today());
    }

    @Override
    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        User user = resolveAccount(request);
        Employee employee = new Employee(user,
                String.format(Locale.ROOT, EMPLOYEE_NO_FORMAT, employeeRepository.nextEmployeeNumber()));
        applyDetails(employee, request);
        employeeRepository.save(employee);

        publish(WorkflowEventType.EMPLOYEE_CREATED, employee, user.getId(), employee.getEmployeeNo(), user.getFullName());
        return EmployeeMapper.toResponse(employee, today());
    }

    @Override
    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        Employee employee = find(id);
        String nic = normalizeNic(request.getNic());
        if (employeeRepository.existsByNicAndIdNot(nic, id)) {
            throw DuplicateRecordException.forField("nic", "nic.duplicate");
        }
        applyDetails(employee, request);
        publish(WorkflowEventType.EMPLOYEE_UPDATED, employee, null, employee.getEmployeeNo(),
                employee.getUser().getFullName());
        return EmployeeMapper.toResponse(employee, today());
    }

    @Override
    @Transactional
    public EmployeeResponse addDocument(Long employeeId, EmployeeDocumentType type, String title, MultipartFile file) {
        Employee employee = find(employeeId);
        if (documentRepository.countByEmployeeId(employeeId) >= MAX_DOCUMENTS) {
            throw BusinessRuleException.forField("file", "employee.documents.max", MAX_DOCUMENTS);
        }
        StoredFile stored = fileStorage.store(file, FileCategory.DOCUMENT, "file");
        employee.addDocument(new EmployeeDocument(employee, type, title, stored));
        employeeRepository.saveAndFlush(employee);
        publish(WorkflowEventType.EMPLOYEE_DOCUMENT_ADDED, employee, null, title, employee.getEmployeeNo());
        return EmployeeMapper.toResponse(employee, today());
    }

    @Override
    @Transactional
    public EmployeeResponse removeDocument(Long employeeId, Long documentId) {
        Employee employee = find(employeeId);
        EmployeeDocument document = findDocument(employeeId, documentId);
        StoredFile file = document.getFile();
        String title = document.getTitle();
        employee.removeDocument(document);
        employeeRepository.saveAndFlush(employee);
        fileStorage.delete(file);
        publish(WorkflowEventType.EMPLOYEE_DOCUMENT_REMOVED, employee, null, title, employee.getEmployeeNo());
        return EmployeeMapper.toResponse(employee, today());
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload openDocument(Long employeeId, Long documentId) {
        return fileStorage.open(findDocument(employeeId, documentId).getFile());
    }

    // ----- helpers -------------------------------------------------------------

    /** Either links an eligible existing account or creates a new WORKER account. */
    private User resolveAccount(EmployeeCreateRequest request) {
        if (request.getExistingUserId() != null) {
            if (request.getEmail() != null) {
                throw BusinessRuleException.forField("email", "employee.account.chooseOne");
            }
            User user = userRepository.findById(request.getExistingUserId())
                    .orElseThrow(() -> BusinessRuleException.forField("existingUserId", "employee.account.notFound"));
            if (!user.isActive() || user.getRole().getName() == RoleName.SUPPLIER) {
                throw BusinessRuleException.forField("existingUserId", "employee.account.notEligible");
            }
            if (employeeRepository.existsByUserId(user.getId())) {
                throw DuplicateRecordException.forField("existingUserId", "employee.account.alreadyLinked");
            }
            return user;
        }
        if (request.getEmail() == null) {
            throw BusinessRuleException.forField("email", "employee.account.emailRequired");
        }
        return userService.createWorkerAccount(request.getFullName(), request.getEmail(), request.getPhone());
    }

    /** Copies shared fields onto the entity, enforcing trade, age and pay rules. */
    private void applyDetails(Employee employee, EmployeeDetailsRequest request) {
        User user = employee.getUser();
        user.updateDetails(request.getFullName(), request.getPhone());

        String trade = request.getTrade();
        List<String> allowedTrades = SystemSettingsManager.getInstance().getSettings().trades();
        if (user.getRole().getName() == RoleName.WORKER && trade == null) {
            throw BusinessRuleException.forField("trade", "employee.trade.required");
        }
        if (trade != null && !allowedTrades.contains(trade)) {
            throw BusinessRuleException.forField("trade", "employee.trade.unknown");
        }

        employee.setNic(normalizeNic(request.getNic()));
        employee.changeDateOfBirth(request.getDateOfBirth(), today());
        employee.setAddress(request.getAddress());
        employee.setJoinDate(request.getJoinDate());
        employee.setTrade(trade);
        employee.setEmergencyContactName(request.getEmergencyContactName());
        employee.setEmergencyContactPhone(request.getEmergencyContactPhone());
        employee.applyPayTerms(request.getEmploymentType(), request.getDailyRate(), request.getMonthlySalary(),
                request.getContractAmount(), request.getContractStartDate(), request.getContractEndDate());
    }

    private void publish(WorkflowEventType type, Employee employee, Long recipientId, Object... args) {
        SiteMasterUserPrincipal actor = currentUser.requirePrincipal();
        eventPublisher.publish(WorkflowEvent.builder(type)
                .actor(actor.getUserId(), actor.getFullName())
                .entityId(employee.getId())
                .args(args)
                .recipients(recipientId)
                .link("profile")
                .build());
    }

    private Employee find(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Employee"));
    }

    private EmployeeDocument findDocument(Long employeeId, Long documentId) {
        return documentRepository.findByIdAndEmployeeId(documentId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Document"));
    }

    private static String normalizeNic(String nic) {
        return nic == null ? null : nic.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
