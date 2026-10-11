package com.sitemaster.budget;

import java.math.BigDecimal;

/**
 * Something that spends a phase's budget. Material requests implement it now;
 * expense requests add their own implementation in Phase 6.
 * <p>
 * OOP concepts: Abstraction and Polymorphism - PhaseBudgetService adds up every
 * implementation without knowing what they are.
 */
public interface PhaseCostSource {

    /**
     * @param phaseId                  phase to total
     * @param excludeMaterialRequestId a request to leave out (the one being checked), or null
     */
    BigDecimal committedCost(Long phaseId, Long excludeMaterialRequestId);
}
