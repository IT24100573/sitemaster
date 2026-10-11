package com.sitemaster.budget.dto;

import java.math.BigDecimal;

/**
 * Budget position of a phase before approving a cost.
 *
 * @param phaseId     phase checked
 * @param phaseName   its name
 * @param phaseBudget phase budget
 * @param committed   already committed by other approved items
 * @param remaining   budget left before this item
 * @param cost        cost of the item being approved
 * @param overrun     true when cost &gt; remaining (show the warning)
 */
public record BudgetCheckResponse(Long phaseId, String phaseName, BigDecimal phaseBudget, BigDecimal committed,
                                  BigDecimal remaining, BigDecimal cost, boolean overrun) {
}
