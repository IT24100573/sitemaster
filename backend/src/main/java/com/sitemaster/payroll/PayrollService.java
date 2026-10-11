package com.sitemaster.payroll;

import com.sitemaster.common.enums.PaymentMethod;
import com.sitemaster.payroll.dto.PayrollPreviewResponse;
import com.sitemaster.payroll.dto.PayrollRunDetail;
import com.sitemaster.payroll.dto.PayrollRunResponse;
import com.sitemaster.payroll.dto.PayslipResponse;

import java.time.YearMonth;
import java.util.List;

/**
 * Monthly payroll: HR previews and sends the attendance summary, the Accountant
 * calculates (Strategy + Decorator) and pays; employees read their payslips.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface PayrollService {

    PayrollPreviewResponse preview(YearMonth month);

    PayrollRunResponse sendSummary(YearMonth month);

    List<PayrollRunResponse> runs();

    PayrollRunDetail run(Long id);

    PayrollRunDetail calculate(Long id);

    PayrollRunDetail payAll(Long id, PaymentMethod method, String reference);

    PayslipResponse paySlip(Long payslipId, PaymentMethod method, String reference);

    List<PayslipResponse> myPayslips();

    PayslipResponse payslip(Long id);

    byte[] payslipPdf(Long id);
}
