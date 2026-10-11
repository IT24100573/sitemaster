package com.sitemaster.report.generator;

import com.sitemaster.payroll.Payslip;
import com.sitemaster.report.ReportCriteria;
import com.sitemaster.report.ReportTable;
import com.sitemaster.report.ReportType;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.sitemaster.report.generator.ReportSupport.col;
import static com.sitemaster.report.generator.ReportSupport.row;
import static com.sitemaster.report.generator.ReportSupport.sum;

/** Payslips of every payroll month that falls in the period (company-wide). */
@Component
public class PayrollReport implements ReportGenerator {

    private final ReportSupport support;

    public PayrollReport(ReportSupport support) {
        this.support = support;
    }

    @Override
    public ReportType type() {
        return ReportType.PAYROLL;
    }

    @Override
    public ReportTable generate(ReportCriteria criteria) {
        int fromKey = criteria.from().getYear() * 100 + criteria.from().getMonthValue();
        int toKey = criteria.to().getYear() * 100 + criteria.to().getMonthValue();
        TypedQuery<Payslip> query = support.em().createQuery("select p from Payslip p join fetch p.payrollRun r join fetch p.employee e "
                + "join fetch e.user where (r.year * 100 + r.month) between :from and :to "
                + "order by r.year, r.month, e.employeeNo", Payslip.class)
                .setParameter("from", fromKey).setParameter("to", toKey);
        List<Payslip> payslips = support.limited(query);
        List<List<Object>> rows = payslips.stream().map(p -> row(
                p.getPayrollRun().getYear() + "-" + String.format("%02d", p.getPayrollRun().getMonth()),
                p.getEmployee().getEmployeeNo(), p.getEmployee().getUser().getFullName(), p.getEmploymentType().name(),
                p.getGrossPay(), p.getTotalDeductions(), p.getNetPay(), p.isPaid() ? "Paid" : "Not paid")).toList();
        return new ReportTable(type().getTitle(), support.period(criteria),
                List.of(col("Month", ReportTable.Kind.TEXT), col("Employee no", ReportTable.Kind.TEXT),
                        col("Name", ReportTable.Kind.TEXT), col("Type", ReportTable.Kind.TEXT),
                        col("Gross", ReportTable.Kind.MONEY), col("Deductions", ReportTable.Kind.MONEY),
                        col("Net", ReportTable.Kind.MONEY), col("Paid", ReportTable.Kind.TEXT)),
                rows, row("Total", null, null, null, sum(payslips, Payslip::getGrossPay),
                        sum(payslips, Payslip::getTotalDeductions), sum(payslips, Payslip::getNetPay), null));
    }
}
