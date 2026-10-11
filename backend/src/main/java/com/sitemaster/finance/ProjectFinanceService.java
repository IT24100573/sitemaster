package com.sitemaster.finance;

import com.sitemaster.finance.dto.ProjectFinanceResponse;

import java.util.List;

/**
 * Budget versus spending, commitments and billing per project.
 * <p>
 * OOP concept: Abstraction - controllers, reports and dashboards depend on this interface.
 */
public interface ProjectFinanceService {

    ProjectFinanceResponse forProject(Long projectId);

    /** Every project the user may see financials for (no phase detail). */
    List<ProjectFinanceResponse> overview();
}
