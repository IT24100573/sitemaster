package com.sitemaster.budget;

import com.sitemaster.budget.dto.BudgetCheckResponse;
import com.sitemaster.project.Phase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Works out how much of a phase's budget is left, by asking every
 * {@link PhaseCostSource}. Used for the PM's budget-overrun warning.
 * <p>
 * OOP concept: Polymorphism - depends only on the PhaseCostSource interface.
 */
@Service
public class PhaseBudgetService {

    private final List<PhaseCostSource> sources;

    public PhaseBudgetService(List<PhaseCostSource> sources) {
        this.sources = sources;
    }

    @Transactional(readOnly = true)
    public BudgetCheckResponse check(Phase phase, BigDecimal cost, Long excludeMaterialRequestId) {
        BigDecimal committed = sources.stream()
                .map(source -> source.committedCost(phase.getId(), excludeMaterialRequestId))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = phase.getBudget().subtract(committed);
        return new BudgetCheckResponse(phase.getId(), phase.getName(), phase.getBudget(), committed, remaining, cost,
                cost.compareTo(remaining) > 0);
    }
}
