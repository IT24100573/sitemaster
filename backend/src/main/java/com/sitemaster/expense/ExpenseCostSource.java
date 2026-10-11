package com.sitemaster.expense;

import com.sitemaster.budget.PhaseCostSource;
import com.sitemaster.common.enums.RequestStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumSet;

/**
 * Money committed to a phase by approved or paid expenses. Plugged into
 * PhaseBudgetService next to material requests.
 * <p>
 * OOP concept: Polymorphism - a second implementation of PhaseCostSource.
 */
@Component
public class ExpenseCostSource implements PhaseCostSource {

    private static final EnumSet<RequestStatus> COMMITTED = EnumSet.of(RequestStatus.APPROVED, RequestStatus.PAID);

    private final ExpenseRepository repository;

    public ExpenseCostSource(ExpenseRepository repository) {
        this.repository = repository;
    }

    @Override
    public BigDecimal committedCost(Long phaseId, Long excludeMaterialRequestId) {
        return repository.sumForPhase(phaseId, COMMITTED);
    }
}
