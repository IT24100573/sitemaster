package com.sitemaster.report.generator;

import com.sitemaster.finance.ProjectFinanceService;
import com.sitemaster.finance.dto.ProjectFinanceResponse;
import com.sitemaster.report.ReportCriteria;
import com.sitemaster.report.ReportTable;
import com.sitemaster.report.ReportType;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sitemaster.report.generator.ReportSupport.col;
import static com.sitemaster.report.generator.ReportSupport.row;
import static com.sitemaster.report.generator.ReportSupport.sum;

/** Budget, spent, committed and billing per project (current position; dates are not applied). */
@Component
public class ProjectFinanceReport implements ReportGenerator {

    private final ProjectFinanceService financeService;

    public ProjectFinanceReport(ProjectFinanceService financeService) {
        this.financeService = financeService;
    }

    @Override
    public ReportType type() {
        return ReportType.PROJECT_FINANCE;
    }

    @Override
    public ReportTable generate(ReportCriteria criteria) {
        List<ProjectFinanceResponse> projects = criteria.projectId() != null
                ? List.of(financeService.forProject(criteria.projectId()))
                : financeService.overview();
        List<List<Object>> rows = projects.stream().map(p -> row(p.projectName(), p.status(), p.budget(),
                p.totalSpent(), p.committed(), p.remaining(), p.usedPercent(), p.invoiced(), p.received())).toList();
        List<Object> totals = row("Total", null, sum(projects, ProjectFinanceResponse::budget),
                sum(projects, ProjectFinanceResponse::totalSpent), sum(projects, ProjectFinanceResponse::committed),
                sum(projects, ProjectFinanceResponse::remaining), null, sum(projects, ProjectFinanceResponse::invoiced),
                sum(projects, ProjectFinanceResponse::received));
        return new ReportTable(type().getTitle(), "Position as of today",
                List.of(col("Project", ReportTable.Kind.TEXT), col("Status", ReportTable.Kind.TEXT),
                        col("Budget", ReportTable.Kind.MONEY), col("Spent", ReportTable.Kind.MONEY),
                        col("Committed", ReportTable.Kind.MONEY), col("Remaining", ReportTable.Kind.MONEY),
                        col("Used", ReportTable.Kind.PERCENT), col("Invoiced", ReportTable.Kind.MONEY),
                        col("Received", ReportTable.Kind.MONEY)),
                rows, totals);
    }
}
