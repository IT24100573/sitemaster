package com.sitemaster.payroll.dto;

import java.util.List;

/** A run with HR's summary rows and the calculated payslips. */
public record PayrollRunDetail(PayrollRunResponse run, List<SummaryRow> summaries, List<PayslipResponse> payslips) {

    public PayrollRunDetail {
        summaries = List.copyOf(summaries);
        payslips = List.copyOf(payslips);
    }
}
