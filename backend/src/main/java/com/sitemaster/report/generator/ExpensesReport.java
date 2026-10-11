package com.sitemaster.report.generator;

import com.sitemaster.expense.ExpenseRequest;
import com.sitemaster.report.ReportCriteria;
import com.sitemaster.report.ReportTable;
import com.sitemaster.report.ReportType;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sitemaster.report.generator.ReportSupport.col;
import static com.sitemaster.report.generator.ReportSupport.row;
import static com.sitemaster.report.generator.ReportSupport.sum;

/** Expense requests dated in the period, on the projects the user can see. */
@Component
public class ExpensesReport implements ReportGenerator {

    private final ReportSupport support;

    public ExpensesReport(ReportSupport support) {
        this.support = support;
    }

    @Override
    public ReportType type() {
        return ReportType.EXPENSES;
    }

    @Override
    public ReportTable generate(ReportCriteria criteria) {
        List<Long> projects = support.projectScope(criteria);
        TypedQuery<ExpenseRequest> query = support.em().createQuery("select e from ExpenseRequest e join fetch e.project "
                + "where e.expenseDate between :from and :to" + (projects == null ? "" : " and e.project.id in :projects")
                + " order by e.expenseDate, e.id", ExpenseRequest.class)
                .setParameter("from", criteria.from()).setParameter("to", criteria.to());
        if (projects != null) {
            query.setParameter("projects", projects);
        }
        List<ExpenseRequest> expenses = support.limited(query);
        List<List<Object>> rows = expenses.stream().map(e -> row(e.getExpenseDate(), e.getProject().getName(),
                e.getCategory().name(), e.getPayee(), e.getDescription(), e.getStatus().name(), e.getAmount())).toList();
        return new ReportTable(type().getTitle(), support.period(criteria),
                List.of(col("Date", ReportTable.Kind.DATE), col("Project", ReportTable.Kind.TEXT),
                        col("Category", ReportTable.Kind.TEXT), col("Payee", ReportTable.Kind.TEXT),
                        col("Description", ReportTable.Kind.TEXT), col("Status", ReportTable.Kind.TEXT),
                        col("Amount", ReportTable.Kind.MONEY)),
                rows, row("Total", null, null, null, null, null, sum(expenses, ExpenseRequest::getAmount)));
    }
}
