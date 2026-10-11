package com.sitemaster.hr;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.employee.Employee;
import com.sitemaster.employee.EmployeeRepository;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileDownload;
import com.sitemaster.file.FileStorageService;
import com.sitemaster.file.StoredFile;
import com.sitemaster.hr.dto.ContractRequest;
import com.sitemaster.hr.dto.ContractResponse;
import com.sitemaster.hr.dto.TrainingRequest;
import com.sitemaster.hr.dto.TrainingResponse;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link HrRecordsService}. Contract numbers are unique;
 * contracts ending within {@value #REMINDER_DAYS} days are flagged and HR is
 * notified once per contract.
 * <p>
 * OOP concept: Abstraction - implements HrRecordsService.
 */
@Service
public class HrRecordsServiceImpl implements HrRecordsService {

    public static final int REMINDER_DAYS = 30;

    private static final Map<String, String> CONTRACT_SORTS = Map.of(
            "endDate", "endDate", "startDate", "startDate", "contractNo", "contractNo");
    private static final Map<String, String> TRAINING_SORTS = Map.of(
            "completedOn", "completedOn", "expiresOn", "expiresOn", "title", "title");

    private final ContractRepository contractRepository;
    private final TrainingRecordRepository trainingRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorage;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public HrRecordsServiceImpl(ContractRepository contractRepository, TrainingRecordRepository trainingRepository,
                                EmployeeRepository employeeRepository, UserRepository userRepository,
                                FileStorageService fileStorage, WorkflowEventPublisher eventPublisher, Clock clock) {
        this.contractRepository = contractRepository;
        this.trainingRepository = trainingRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.fileStorage = fileStorage;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // ----- contracts ---------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContractResponse> contracts(Long employeeId, Integer expiringWithinDays, PageQuery pageQuery) {
        LocalDate today = LocalDate.now(clock);
        Specification<Contract> spec = (root, q, cb) ->
                employeeId == null ? null : cb.equal(root.get("employee").get("id"), employeeId);
        spec = spec.and((root, q, cb) -> expiringWithinDays == null ? null
                        : cb.between(root.get("endDate"), today, today.plusDays(expiringWithinDays)))
                .and(employeeSearch(pageQuery.getSearch(), "contractNo"));
        return PageResponse.from(contractRepository.findAll(spec,
                pageQuery.toPageable(CONTRACT_SORTS, "endDate", Sort.Direction.ASC)), this::toResponse);
    }

    @Override
    @Transactional
    public ContractResponse createContract(ContractRequest request) {
        if (contractRepository.existsByContractNoIgnoreCase(request.getContractNo())) {
            throw DuplicateRecordException.forField("contractNo", "contract.number.duplicate");
        }
        Contract contract = new Contract(employee(request.getEmployeeId()));
        applyContract(contract, request);
        contractRepository.save(contract);
        publish(WorkflowEventType.CONTRACT_SAVED, contract.getId(), contract.getContractNo(),
                contract.getEmployee().getUser().getFullName());
        return toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse updateContract(Long id, ContractRequest request) {
        Contract contract = findContract(id);
        if (contractRepository.existsByContractNoIgnoreCaseAndIdNot(request.getContractNo(), id)) {
            throw DuplicateRecordException.forField("contractNo", "contract.number.duplicate");
        }
        applyContract(contract, request);
        publish(WorkflowEventType.CONTRACT_SAVED, id, contract.getContractNo(),
                contract.getEmployee().getUser().getFullName());
        return toResponse(contract);
    }

    @Override
    @Transactional
    public ContractResponse uploadContractFile(Long id, MultipartFile file) {
        Contract contract = findContract(id);
        StoredFile old = contract.replaceFile(fileStorage.store(file, FileCategory.DOCUMENT, "file"));
        contractRepository.saveAndFlush(contract);
        if (old != null) {
            fileStorage.delete(old);
        }
        return toResponse(contract);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload openContractFile(Long id) {
        Contract contract = findContract(id);
        if (contract.getFile() == null) {
            throw new ResourceNotFoundException("resource.notFound", "Contract file");
        }
        return fileStorage.open(contract.getFile());
    }

    private void applyContract(Contract contract, ContractRequest r) {
        contract.update(employee(r.getEmployeeId()), r.getContractNo(), r.getTitle(), r.getStartDate(), r.getEndDate(),
                r.getContractValue(), r.getNotes());
    }

    // ----- training ----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TrainingResponse> training(Long employeeId, Boolean safety, PageQuery pageQuery) {
        Specification<TrainingRecord> spec = (root, q, cb) ->
                employeeId == null ? null : cb.equal(root.get("employee").get("id"), employeeId);
        spec = spec.and((root, q, cb) -> safety == null ? null : cb.equal(root.get("safety"), safety))
                .and(employeeSearch(pageQuery.getSearch(), "title"));
        return PageResponse.from(trainingRepository.findAll(spec,
                pageQuery.toPageable(TRAINING_SORTS, "completedOn", Sort.Direction.DESC)), this::toResponse);
    }

    @Override
    @Transactional
    public TrainingResponse createTraining(TrainingRequest request) {
        TrainingRecord record = new TrainingRecord(employee(request.getEmployeeId()));
        applyTraining(record, request);
        trainingRepository.save(record);
        publish(WorkflowEventType.TRAINING_SAVED, record.getId(), record.getTitle(),
                record.getEmployee().getUser().getFullName());
        return toResponse(record);
    }

    @Override
    @Transactional
    public TrainingResponse updateTraining(Long id, TrainingRequest request) {
        TrainingRecord record = findTraining(id);
        applyTraining(record, request);
        publish(WorkflowEventType.TRAINING_SAVED, id, record.getTitle(), record.getEmployee().getUser().getFullName());
        return toResponse(record);
    }

    @Override
    @Transactional
    public TrainingResponse uploadCertificate(Long id, MultipartFile file) {
        TrainingRecord record = findTraining(id);
        StoredFile old = record.replaceFile(fileStorage.store(file, FileCategory.DOCUMENT, "file"));
        trainingRepository.saveAndFlush(record);
        if (old != null) {
            fileStorage.delete(old);
        }
        return toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload openCertificate(Long id) {
        TrainingRecord record = findTraining(id);
        if (record.getFile() == null) {
            throw new ResourceNotFoundException("resource.notFound", "Certificate");
        }
        return fileStorage.open(record.getFile());
    }

    @Override
    @Transactional
    public void deleteTraining(Long id) {
        TrainingRecord record = findTraining(id);
        StoredFile file = record.getFile();
        publish(WorkflowEventType.TRAINING_DELETED, id, record.getTitle(), record.getEmployee().getUser().getFullName());
        trainingRepository.delete(record);
        trainingRepository.flush();
        if (file != null) {
            fileStorage.delete(file);
        }
    }

    private void applyTraining(TrainingRecord record, TrainingRequest r) {
        record.update(employee(r.getEmployeeId()), r.getTitle(), r.getProvider(), r.isSafety(), r.getCompletedOn(),
                r.getExpiresOn(), r.getNotes());
    }

    // ----- reminders -----------------------------------------------------------------------

    @Override
    @Transactional
    public int sendRenewalReminders() {
        LocalDate today = LocalDate.now(clock);
        List<Contract> due = contractRepository.findDueForReminder(today, today.plusDays(REMINDER_DAYS));
        List<Long> hr = userRepository.findActiveIdsByRole(RoleName.HR_MANAGER);
        for (Contract contract : due) {
            eventPublisher.publish(WorkflowEvent.builder(WorkflowEventType.CONTRACT_EXPIRING)
                    .entityId(contract.getId())
                    .args(contract.getContractNo(), contract.getEmployee().getUser().getFullName(), contract.getEndDate())
                    .recipients(hr).link("contracts").build());
            contract.markReminded(LocalDateTime.now(clock));
        }
        return due.size();
    }

    // ----- helpers -------------------------------------------------------------------------

    private static <T> Specification<T> employeeSearch(String search, String ownField) {
        return (root, q, cb) -> {
            if (search == null) {
                return null;
            }
            Join<Object, Object> employee = root.join("employee");
            Join<Object, Object> user = employee.join("user");
            String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
            return cb.or(cb.like(cb.lower(user.get("fullName")), like),
                    cb.like(cb.lower(employee.get("employeeNo")), like),
                    cb.like(cb.lower(root.get(ownField)), like));
        };
    }

    private void publish(WorkflowEventType type, Long id, Object... args) {
        eventPublisher.publish(WorkflowEvent.byCurrentUser(type).entityId(id).args(args).build());
    }

    private Employee employee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Employee"));
    }

    private Contract findContract(Long id) {
        return contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Contract"));
    }

    private TrainingRecord findTraining(Long id) {
        return trainingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Training record"));
    }

    private ContractResponse toResponse(Contract c) {
        LocalDate today = LocalDate.now(clock);
        Contract.Status status = c.statusOn(today);
        long days = c.daysToExpiry(today);
        Employee e = c.getEmployee();
        return new ContractResponse(c.getId(), e.getId(), e.getEmployeeNo(), e.getUser().getFullName(),
                c.getContractNo(), c.getTitle(), c.getStartDate(), c.getEndDate(), c.getContractValue(), c.getNotes(),
                status.name(), days, status != Contract.Status.EXPIRED && days <= REMINDER_DAYS,
                c.getFile() == null ? null : c.getFile().getOriginalName());
    }

    private TrainingResponse toResponse(TrainingRecord t) {
        Employee e = t.getEmployee();
        return new TrainingResponse(t.getId(), e.getId(), e.getEmployeeNo(), e.getUser().getFullName(), t.getTitle(),
                t.getProvider(), t.isSafety(), t.getCompletedOn(), t.getExpiresOn(), t.isExpired(LocalDate.now(clock)),
                t.getNotes(), t.getFile() == null ? null : t.getFile().getOriginalName());
    }
}
