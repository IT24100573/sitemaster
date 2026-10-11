package com.sitemaster.report.generator;

import com.sitemaster.purchase.Payment;
import com.sitemaster.report.ReportCriteria;
import com.sitemaster.report.ReportTable;
import com.sitemaster.report.ReportType;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sitemaster.report.generator.ReportSupport.col;
import static com.sitemaster.report.generator.ReportSupport.row;
import static com.sitemaster.report.generator.ReportSupport.sum;

/** Money paid out in the period: supplier invoices and expenses. */
@Component
public class PaymentsReport implements ReportGenerator {

    private final ReportSupport support;

    public PaymentsReport(ReportSupport support) {
        this.support = support;
    }

    @Override
    public ReportType type() {
        return ReportType.PAYMENTS;
    }

    @Override
    public ReportTable generate(ReportCriteria criteria) {
        boolean oneProject = criteria.projectId() != null;
        if (oneProject) {
            support.projectScope(criteria);
        }
        String jpql = "select p from Payment p left join fetch p.supplierInvoice si left join fetch si.purchaseOrder po "
                + "left join fetch p.expenseRequest e where p.paymentDate between :from and :to"
                + (oneProject ? " and (po.project.id = :project or e.project.id = :project)" : "")
                + " order by p.paymentDate, p.id";
        TypedQuery<Payment> query = support.em().createQuery(jpql, Payment.class)
                .setParameter("from", criteria.from()).setParameter("to", criteria.to());
        if (oneProject) {
            query.setParameter("project", criteria.projectId());
        }
        List<Payment> payments = support.limited(query);
        List<List<Object>> rows = payments.stream().map(p -> {
            boolean supplier = p.getSupplierInvoice() != null;
            String paidTo = supplier ? p.getSupplierInvoice().getSupplier().getName() : p.getExpenseRequest().getPayee();
            String project = supplier ? p.getSupplierInvoice().getPurchaseOrder().getProject().getName()
                    : p.getExpenseRequest().getProject().getName();
            String document = supplier ? p.getSupplierInvoice().getInvoiceNumber() : "EXP-" + p.getExpenseRequest().getId();
            return row(p.getPaymentDate(), supplier ? "Supplier invoice" : "Expense", paidTo, project, document,
                    p.getMethod().name(), p.getReferenceNo(), p.getAmount());
        }).toList();
        return new ReportTable(type().getTitle(), support.period(criteria),
                List.of(col("Date", ReportTable.Kind.DATE), col("Type", ReportTable.Kind.TEXT),
                        col("Paid to", ReportTable.Kind.TEXT), col("Project", ReportTable.Kind.TEXT),
                        col("Document", ReportTable.Kind.TEXT), col("Method", ReportTable.Kind.TEXT),
                        col("Reference", ReportTable.Kind.TEXT), col("Amount", ReportTable.Kind.MONEY)),
                rows, row("Total", null, null, null, null, null, null, sum(payments, Payment::getAmount)));
    }
}
