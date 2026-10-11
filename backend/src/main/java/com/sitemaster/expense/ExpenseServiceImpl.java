package com.sitemaster.expense;

import com.sitemaster.budget.PhaseBudgetService;
import com.sitemaster.budget.dto.BudgetCheckResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.ExpenseCategory;
import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.common.enums.PhaseStatus;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.expense.dto.ExpenseForm;
import com.sitemaster.expense.dto.ExpensePaymentRequest;
import com.sitemaster.expense.dto.ExpenseResponse;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileDownload;
import com.sitemaster.file.FileStorageService;
import com.sitemaster.pettycash.PettyCashService;
import com.sitemaster.pettycash.PettyCashTransactionRepository;
import com.sitemaster.project.Phase;
import com.sitemaster.project.PhaseRepository;
import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.purchase.Payment;
import com.sitemaster.purchase.PaymentRepository;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.settings.SystemSettingsManager;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of {@link ExpenseService}. Rules:
 * <ul>
 *   <li>a site engineer on the project, or the Accountant, raises an expense; amount &gt; 0,
 *       date not in the future, phase of this project and not completed;</li>
 *   <li>a receipt is required above the receipt threshold in System Settings;</li>
 *   <li>only the project's own PM approves or rejects, never their own request; an
 *       amount above the phase's remaining budget needs the overrun acknowledged;</li>
 *   <li>only the Accountant pays, only APPROVED expenses, once; bank/cheque references
 *       are unique; petty cash payments come out of the project's float.</li>
 * </ul>
 * OOP concept: Abstraction - implements ExpenseService.
 */
@Service
public class ExpenseServiceImpl implements ExpenseService {

    private static final Map<String, String> SORTS = Map.of("createdAt", "createdAt", "expenseDate", "expenseDate",
            "amount", "amount", "status", "status");

    private final ExpenseRepository expenseRepository;
    private final PhaseRepository phaseRepository;
    private final PaymentRepository paymentRepository;
    private final PettyCashTransactionRepository pettyCashTransactions;
    private final UserRepository userRepository;
    private final PhaseBudgetService budgetService;
    private final PettyCashService pettyCashService;
    private final FileStorageService fileStorage;
    private final ProjectAccess access;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository, PhaseRepository phaseRepository,
                              PaymentRepository paymentRepository, PettyCashTransactionRepository pettyCashTransactions,
                              UserRepository userRepository, PhaseBudgetService budgetService,
                              PettyCashService pettyCashService, FileStorageService fileStorage, ProjectAccess access,
                              CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher, Clock clock) {
        this.expenseRepository = expenseRepository;
        this.phaseRepository = phaseRepository;
        this.paymentRepository = paymentRepository;
        this.pettyCashTransactions = pettyCashTransactions;
        this.userRepository = userRepository;
        this.budgetService = budgetService;
        this.pettyCashService = pettyCashService;
        this.fileStorage = fileStorage;
        this.access = access;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> search(Long projectId, RequestStatus status, ExpenseCategory category,
                                                boolean mineOnly, PageQuery pageQuery) {
        if (projectId != null) {
            access.requireVisible(projectId);
        }
        Long me = currentUser.requireId();
        String search = pageQuery.getSearch();
        Specification<ExpenseRequest> spec = Specification.where(access.<ExpenseRequest>visibleThrough("project"));
        spec = spec.and((root, q, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId))
                .and((root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status))
                .and((root, q, cb) -> category == null ? null : cb.equal(root.get("category"), category))
                .and((root, q, cb) -> mineOnly ? cb.equal(root.get("requestedBy").get("id"), me) : null)
                .and((root, q, cb) -> {
                    if (search == null) {
                        return null;
                    }
                    String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                    return cb.or(cb.like(cb.lower(root.get("payee")), like), cb.like(cb.lower(root.get("description")), like));
                });
        return PageResponse.from(expenseRepository.findAll(spec, pageQuery.toPageable(SORTS, "createdAt", Sort.Direction.DESC)),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse get(Long id) {
        ExpenseRequest expense = find(id);
        access.requireVisible(expense.getProject().getId());
        return toResponse(expense);
    }

    @Override
    @Transactional
    public ExpenseResponse create(Long projectId, ExpenseForm form) {
        Project project = access.requireParticipant(projectId);
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        boolean engineer = me.getRole() == RoleName.SITE_ENGINEER && access.isMember(project);
        if (!engineer && me.getRole() != RoleName.ACCOUNTANT) {
            throw new UnauthorizedActionException("expense.create.denied");
        }
        Phase phase = phaseRepository.findById(form.getPhaseId())
                .filter(p -> p.getProject().getId().equals(projectId))
                .orElseThrow(() -> BusinessRuleException.forField("phaseId", "phase.otherProject"));
        if (phase.getStatus() == PhaseStatus.COMPLETED) {
            throw BusinessRuleException.forField("phaseId", "task.phase.completed");
        }
        ExpenseRequest expense = new ExpenseRequest(project, phase, form.getCategory(), form.getAmount(),
                form.getExpenseDate(), form.getPayee(), form.getDescription(), currentUser.requireUser());
        BigDecimal threshold = SystemSettingsManager.getInstance().getSettings().receiptThreshold();
        boolean hasReceipt = form.getReceipt() != null && !form.getReceipt().isEmpty();
        if (!hasReceipt) {
            expense.requireReceipt(threshold);
        }
        if (hasReceipt) {
            expense.attachReceipt(fileStorage.store(form.getReceipt(), FileCategory.DOCUMENT, "receipt"));
        }
        expenseRepository.save(expense);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.EXPENSE_SUBMITTED)
                .entityId(expense.getId()).args(expense.getAmount(), expense.getPayee(), project.getName())
                .recipients(project.getManager().getId()).link("expenses/" + expense.getId()).build());
        return toResponse(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetCheckResponse budgetCheck(Long id) {
        ExpenseRequest expense = find(id);
        access.requireVisible(expense.getProject().getId());
        return budgetService.check(expense.getPhase(), expense.getAmount(), null);
    }

    @Override
    @Transactional
    public ExpenseResponse approve(Long id, String remarks, boolean acknowledgeOverrun) {
        ExpenseRequest expense = requireReviewable(id);
        BudgetCheckResponse budget = budgetService.check(expense.getPhase(), expense.getAmount(), null);
        expense.prepareBudgetCheck(budget.remaining(), acknowledgeOverrun);
        expense.approve(currentUser.requireUser(), remarks);
        expense.recordDecision(LocalDateTime.now(clock));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.EXPENSE_APPROVED)
                .entityId(id).args(expense.getAmount(), expense.getPayee(), expense.getProject().getName())
                .recipients(expense.getRequestedBy().getId())
                .recipients(userRepository.findActiveIdsByRole(RoleName.ACCOUNTANT))
                .link("expenses/" + id).build());
        return toResponse(expense);
    }

    @Override
    @Transactional
    public ExpenseResponse reject(Long id, String remarks) {
        ExpenseRequest expense = requireReviewable(id);
        expense.reject(currentUser.requireUser(), remarks);
        expense.recordDecision(LocalDateTime.now(clock));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.EXPENSE_REJECTED)
                .entityId(id).args(expense.getAmount(), expense.getPayee(), expense.getRemarks())
                .recipients(expense.getRequestedBy().getId()).link("expenses/" + id).build());
        return toResponse(expense);
    }

    @Override
    @Transactional
    public ExpenseResponse pay(Long id, ExpensePaymentRequest body) {
        if (currentUser.requirePrincipal().getRole() != RoleName.ACCOUNTANT) {
            throw new UnauthorizedActionException("expense.pay.denied");
        }
        ExpenseRequest expense = find(id);
        access.requireParticipant(expense.getProject().getId());
        LocalDateTime now = LocalDateTime.now(clock);
        expense.markPaid(now);

        PaymentMethod method = body.isFromPettyCash() ? PaymentMethod.CASH : body.getMethod();
        String reference = method == PaymentMethod.CASH ? null : body.getReferenceNo();
        if (reference != null && paymentRepository.existsByMethodAndReferenceNoIgnoreCase(method, reference)) {
            throw DuplicateRecordException.forField("referenceNo", "payment.reference.duplicate");
        }
        if (body.isFromPettyCash()) {
            pettyCashService.spendForExpense(expense, body.getPaymentDate());
        }
        Payment payment = new Payment(expense.getAmount(), method, reference, body.getPaymentDate(), body.getNotes(),
                currentUser.requireUser());
        payment.settle(expense);
        paymentRepository.save(payment);

        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.EXPENSE_PAID)
                .entityId(id).args(expense.getAmount(), expense.getPayee(), expense.getProject().getName())
                .recipients(expense.getRequestedBy().getId()).link("expenses/" + id).build());
        return toResponse(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload openReceipt(Long id) {
        ExpenseRequest expense = find(id);
        access.requireVisible(expense.getProject().getId());
        if (expense.getReceipt() == null) {
            throw new ResourceNotFoundException("resource.notFound", "Receipt");
        }
        return fileStorage.open(expense.getReceipt());
    }

    private ExpenseRequest requireReviewable(Long id) {
        ExpenseRequest expense = find(id);
        Project project = access.requireParticipant(expense.getProject().getId());
        if (!project.isManagedBy(currentUser.requireId())) {
            throw new UnauthorizedActionException("expense.review.denied");
        }
        return expense;
    }

    private ExpenseRequest find(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Expense"));
    }

    private ExpenseResponse toResponse(ExpenseRequest e) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        Optional<Payment> payment = paymentRepository.findFirstByExpenseRequestId(e.getId());
        boolean pettyCash = e.getStatus() == RequestStatus.PAID
                && pettyCashTransactions.findFirstByExpenseRequestId(e.getId()).isPresent();
        BigDecimal threshold = SystemSettingsManager.getInstance().getSettings().receiptThreshold();
        boolean canReview = e.getStatus() == RequestStatus.PENDING && e.getProject().isManagedBy(me.getUserId())
                && !e.getRequestedBy().getId().equals(me.getUserId());
        boolean canPay = e.getStatus() == RequestStatus.APPROVED && me.getRole() == RoleName.ACCOUNTANT;
        return new ExpenseResponse(e.getId(), e.getProject().getId(), e.getProject().getName(), e.getPhase().getId(),
                e.getPhase().getName(), e.getCategory().name(), e.getAmount(), e.getExpenseDate(), e.getPayee(),
                e.getDescription(), e.getReceipt() == null ? null : e.getReceipt().getOriginalName(),
                threshold != null && e.getAmount().compareTo(threshold) > 0, e.getStatus().name(),
                e.getRequestedBy().getId(), e.getRequestedBy().getFullName(),
                e.getApprovedBy() == null ? null : e.getApprovedBy().getFullName(), e.getRemarks(), e.getDecidedAt(),
                e.getPaidAt(), payment.map(p -> p.getMethod().name()).orElse(null),
                payment.map(Payment::getReferenceNo).orElse(null), pettyCash, canReview, canPay, e.getCreatedAt());
    }
}
