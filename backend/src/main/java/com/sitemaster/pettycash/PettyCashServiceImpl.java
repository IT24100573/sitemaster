package com.sitemaster.pettycash;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.expense.ExpenseRequest;
import com.sitemaster.pettycash.dto.AdjustmentRequest;
import com.sitemaster.pettycash.dto.FundRequest;
import com.sitemaster.pettycash.dto.FundResponse;
import com.sitemaster.pettycash.dto.TopUpRequest;
import com.sitemaster.pettycash.dto.TransactionResponse;
import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.project.ProjectMemberRepository;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.user.User;
import com.sitemaster.user.UserRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link PettyCashService}. Rules:
 * <ul>
 *   <li>one fund per project; the custodian must be on the project team;</li>
 *   <li>top-ups never take the balance above the float limit;</li>
 *   <li>spending never takes it below zero; adjustments need a reason;</li>
 *   <li>project members can see their project's fund; only the Accountant changes it.</li>
 * </ul>
 * OOP concept: Abstraction - implements PettyCashService.
 */
@Service
public class PettyCashServiceImpl implements PettyCashService {

    private static final Map<String, String> SORTS = Map.of("date", "transactionDate", "createdAt", "createdAt");

    private final PettyCashFundRepository fundRepository;
    private final PettyCashTransactionRepository transactionRepository;
    private final ProjectMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final ProjectAccess access;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;

    public PettyCashServiceImpl(PettyCashFundRepository fundRepository,
                                PettyCashTransactionRepository transactionRepository,
                                ProjectMemberRepository memberRepository, UserRepository userRepository,
                                ProjectAccess access, CurrentUserProvider currentUser,
                                WorkflowEventPublisher eventPublisher) {
        this.fundRepository = fundRepository;
        this.transactionRepository = transactionRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FundResponse> funds() {
        Specification<PettyCashFund> spec = Specification.where(access.<PettyCashFund>visibleThrough("project"));
        return fundRepository.findAll(spec).stream()
                .sorted(Comparator.comparing(f -> f.getProject().getName()))
                .map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public FundResponse fund(Long projectId) {
        access.requireVisible(projectId);
        return toResponse(find(projectId));
    }

    @Override
    @Transactional
    public FundResponse saveFund(Long projectId, FundRequest request) {
        requireAccountant();
        Project project = access.requireParticipant(projectId);
        User custodian = userRepository.findById(request.getCustodianId())
                .filter(User::isActive)
                .filter(u -> memberRepository.existsByProjectIdAndUserId(projectId, u.getId()))
                .orElseThrow(() -> BusinessRuleException.forField("custodianId", "pettyCash.custodian.invalid"));
        PettyCashFund fund = fundRepository.findByProjectId(projectId).orElseGet(() -> new PettyCashFund(project));
        fund.configure(custodian, request.getFloatLimit(), request.isActive());
        fundRepository.save(fund);
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PETTY_CASH_FUND_SAVED)
                .entityId(fund.getId()).args(project.getName(), fund.getFloatLimit(), custodian.getFullName()).build());
        return toResponse(fund);
    }

    @Override
    @Transactional
    public FundResponse topUp(Long projectId, TopUpRequest request) {
        requireAccountant();
        access.requireParticipant(projectId);
        PettyCashFund fund = find(projectId);
        fund.topUp(request.getAmount());
        transactionRepository.save(new PettyCashTransaction(fund, PettyCashTransaction.Type.TOP_UP, request.getAmount(),
                request.getDate(), request.getDescription(), null, currentUser.requireUser()));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PETTY_CASH_TOPPED_UP)
                .entityId(fund.getId()).args(fund.getProject().getName(), request.getAmount(), fund.getBalance())
                .recipients(fund.getCustodian().getId()).link("petty-cash").build());
        return toResponse(fund);
    }

    @Override
    @Transactional
    public FundResponse adjust(Long projectId, AdjustmentRequest request) {
        requireAccountant();
        access.requireParticipant(projectId);
        PettyCashFund fund = find(projectId);
        BigDecimal difference = fund.adjustTo(request.getCountedBalance());
        if (difference.signum() == 0) {
            throw BusinessRuleException.forField("countedBalance", "pettyCash.count.same");
        }
        transactionRepository.save(new PettyCashTransaction(fund, PettyCashTransaction.Type.ADJUSTMENT, difference,
                request.getDate(), request.getReason(), null, currentUser.requireUser()));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.PETTY_CASH_ADJUSTED)
                .entityId(fund.getId()).args(fund.getProject().getName(), difference, request.getReason())
                .recipients(fund.getCustodian().getId()).link("petty-cash").build());
        return toResponse(fund);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> transactions(Long projectId, PageQuery pageQuery) {
        access.requireVisible(projectId);
        PettyCashFund fund = find(projectId);
        return PageResponse.from(transactionRepository.findByFundId(fund.getId(),
                pageQuery.toPageable(SORTS, "createdAt", Sort.Direction.DESC)), PettyCashServiceImpl::toResponse);
    }

    @Override
    @Transactional
    public void spendForExpense(ExpenseRequest expense, LocalDate date) {
        PettyCashFund fund = fundRepository.findByProjectId(expense.getProject().getId())
                .orElseThrow(() -> BusinessRuleException.forField("fromPettyCash", "pettyCash.none"));
        fund.spend(expense.getAmount());
        transactionRepository.save(new PettyCashTransaction(fund, PettyCashTransaction.Type.EXPENSE,
                expense.getAmount().negate(), date, expense.getPayee() + ": " + truncate(expense.getDescription()),
                expense, currentUser.requireUser()));
    }

    private static String truncate(String text) {
        return text.length() > 200 ? text.substring(0, 200) : text;
    }

    private void requireAccountant() {
        if (currentUser.requirePrincipal().getRole() != RoleName.ACCOUNTANT) {
            throw new UnauthorizedActionException("pettyCash.accountantOnly");
        }
    }

    private PettyCashFund find(Long projectId) {
        return fundRepository.findByProjectId(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Petty cash fund"));
    }

    private FundResponse toResponse(PettyCashFund f) {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        return new FundResponse(f.getId(), f.getProject().getId(), f.getProject().getName(), f.getCustodian().getId(),
                f.getCustodian().getFullName(), f.getFloatLimit(), f.getBalance(), f.isActive(),
                me.getRole() == RoleName.ACCOUNTANT);
    }

    private static TransactionResponse toResponse(PettyCashTransaction t) {
        return new TransactionResponse(t.getId(), t.getType().name(), t.getAmount(), t.getBalanceAfter(),
                t.getTransactionDate(), t.getDescription(),
                t.getExpenseRequest() == null ? null : t.getExpenseRequest().getId(), t.getRecordedBy().getFullName(),
                t.getCreatedAt());
    }
}
