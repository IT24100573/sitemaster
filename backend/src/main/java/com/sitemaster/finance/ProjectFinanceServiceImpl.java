package com.sitemaster.finance;

import com.sitemaster.budget.PhaseBudgetService;
import com.sitemaster.budget.dto.BudgetCheckResponse;
import com.sitemaster.common.enums.ExpenseCategory;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.expense.ExpenseRepository;
import com.sitemaster.finance.dto.ProjectFinanceResponse;
import com.sitemaster.pettycash.PettyCashFund;
import com.sitemaster.pettycash.PettyCashFundRepository;
import com.sitemaster.project.PhaseRepository;
import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.project.ProjectRepository;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link ProjectFinanceService}. Admin and the Accountant see
 * every project; a PM sees the projects they manage. Material spending counts
 * supplier payments; the MATERIALS category also includes material expenses.
 * <p>
 * OOP concept: Abstraction - implements ProjectFinanceService.
 */
@Service
public class ProjectFinanceServiceImpl implements ProjectFinanceService {

    private static final Set<RoleName> FINANCE_ROLES =
            EnumSet.of(RoleName.ADMIN, RoleName.ACCOUNTANT, RoleName.PROJECT_MANAGER);

    private final FinanceQueries queries;
    private final ExpenseRepository expenseRepository;
    private final PhaseRepository phaseRepository;
    private final ProjectRepository projectRepository;
    private final PettyCashFundRepository fundRepository;
    private final PhaseBudgetService budgetService;
    private final ProjectAccess access;
    private final CurrentUserProvider currentUser;

    public ProjectFinanceServiceImpl(FinanceQueries queries, ExpenseRepository expenseRepository,
                                     PhaseRepository phaseRepository, ProjectRepository projectRepository,
                                     PettyCashFundRepository fundRepository, PhaseBudgetService budgetService,
                                     ProjectAccess access, CurrentUserProvider currentUser) {
        this.queries = queries;
        this.expenseRepository = expenseRepository;
        this.phaseRepository = phaseRepository;
        this.projectRepository = projectRepository;
        this.fundRepository = fundRepository;
        this.budgetService = budgetService;
        this.access = access;
        this.currentUser = currentUser;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectFinanceResponse forProject(Long projectId) {
        requireFinanceRole();
        Project project = access.requireVisible(projectId);
        List<ProjectFinanceResponse.PhaseBudget> phases = phaseRepository.findByProjectIdOrderBySortOrderAscStartDateAsc(projectId)
                .stream().map(phase -> {
                    BudgetCheckResponse check = budgetService.check(phase, BigDecimal.ZERO, null);
                    return new ProjectFinanceResponse.PhaseBudget(phase.getId(), phase.getName(), phase.getBudget(),
                            check.committed(), check.remaining());
                }).toList();
        return build(project, phases);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectFinanceResponse> overview() {
        requireFinanceRole();
        return projectRepository.findAll(access.visibleProjects()).stream()
                .sorted(Comparator.comparing(Project::getName))
                .map(p -> build(p, List.of()))
                .toList();
    }

    private ProjectFinanceResponse build(Project project, List<ProjectFinanceResponse.PhaseBudget> phases) {
        Long id = project.getId();
        BigDecimal materials = queries.supplierPayments(id);
        BigDecimal expenses = queries.paidExpenses(id);
        BigDecimal spent = materials.add(expenses);
        BigDecimal committed = queries.committed(id);
        BigDecimal budget = project.getBudget();
        BigDecimal remaining = budget.subtract(spent).subtract(committed);
        int usedPercent = budget.signum() == 0 ? 0 : spent.add(committed).multiply(BigDecimal.valueOf(100))
                .divide(budget, 0, RoundingMode.HALF_UP).intValue();
        BigDecimal invoiced = queries.invoiced(id);
        BigDecimal received = queries.received(id);

        Map<ExpenseCategory, BigDecimal> byCategory = new EnumMap<>(ExpenseCategory.class);
        for (ExpenseCategory category : ExpenseCategory.values()) {
            byCategory.put(category, BigDecimal.ZERO);
        }
        byCategory.merge(ExpenseCategory.MATERIALS, materials, BigDecimal::add);
        for (Object[] row : expenseRepository.paidByCategory(id)) {
            byCategory.merge((ExpenseCategory) row[0], (BigDecimal) row[1], BigDecimal::add);
        }
        List<ProjectFinanceResponse.CategoryAmount> categories = new ArrayList<>();
        byCategory.forEach((category, amount) -> categories.add(new ProjectFinanceResponse.CategoryAmount(category.name(), amount)));

        BigDecimal petty = fundRepository.findByProjectId(id).map(PettyCashFund::getBalance).orElse(null);
        return new ProjectFinanceResponse(id, project.getName(), project.getStatus().name(), budget, materials, expenses,
                spent, committed, remaining, usedPercent, invoiced, received, invoiced.subtract(received), petty,
                categories, phases);
    }

    private void requireFinanceRole() {
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        if (!FINANCE_ROLES.contains(me.getRole())) {
            throw new UnauthorizedActionException("finance.denied");
        }
    }
}
