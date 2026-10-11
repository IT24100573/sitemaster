package com.sitemaster.finance.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Money position of one project.
 *
 * @param budget          project budget
 * @param spentMaterials  supplier payments for this project's purchase orders
 * @param spentExpenses   paid expense requests
 * @param totalSpent      spentMaterials + spentExpenses
 * @param committed       approved but not yet paid (material requests + expenses)
 * @param remaining       budget - spent - committed (negative = over budget)
 * @param usedPercent     (spent + committed) / budget x 100
 * @param invoiced        total of issued client invoices
 * @param received        money received from the client
 * @param receivable      invoiced - received
 */
public record ProjectFinanceResponse(Long projectId,
                                     String projectName,
                                     String status,
                                     BigDecimal budget,
                                     BigDecimal spentMaterials,
                                     BigDecimal spentExpenses,
                                     BigDecimal totalSpent,
                                     BigDecimal committed,
                                     BigDecimal remaining,
                                     int usedPercent,
                                     BigDecimal invoiced,
                                     BigDecimal received,
                                     BigDecimal receivable,
                                     BigDecimal pettyCashBalance,
                                     List<CategoryAmount> byCategory,
                                     List<PhaseBudget> phases) {

    public ProjectFinanceResponse {
        byCategory = List.copyOf(byCategory);
        phases = List.copyOf(phases);
    }

    /** Spending per expense category. */
    public record CategoryAmount(String category, BigDecimal amount) {
    }

    /** One phase's budget position. */
    public record PhaseBudget(Long phaseId, String name, BigDecimal budget, BigDecimal committed, BigDecimal remaining) {
    }
}
