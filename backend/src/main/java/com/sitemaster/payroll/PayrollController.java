package com.sitemaster.payroll;

import com.sitemaster.payroll.dto.PayrollMonthRequest;
import com.sitemaster.payroll.dto.PayrollPreviewResponse;
import com.sitemaster.payroll.dto.PayrollRunDetail;
import com.sitemaster.payroll.dto.PayrollRunResponse;
import com.sitemaster.payroll.dto.PayslipResponse;
import com.sitemaster.payroll.dto.SalaryPaymentRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

/** Payroll runs and payslips. */
@RestController
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/api/payroll/preview")
    @PreAuthorize("hasAuthority('PAYROLL:CREATE')")
    public PayrollPreviewResponse preview(@RequestParam @Min(value = 2000, message = "{year.range}") @Max(value = 2100, message = "{year.range}") int year,
                                          @RequestParam @Min(value = 1, message = "{month.range}") @Max(value = 12, message = "{month.range}") int month) {
        return payrollService.preview(YearMonth.of(year, month));
    }

    @PostMapping("/api/payroll/runs")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('PAYROLL:CREATE')")
    public PayrollRunResponse sendSummary(@Valid @RequestBody PayrollMonthRequest request) {
        return payrollService.sendSummary(YearMonth.of(request.getYear(), request.getMonth()));
    }

    @GetMapping("/api/payroll/runs")
    @PreAuthorize("hasAuthority('PAYROLL:VIEW')")
    public List<PayrollRunResponse> runs() {
        return payrollService.runs();
    }

    @GetMapping("/api/payroll/runs/{id}")
    @PreAuthorize("hasAuthority('PAYROLL:VIEW')")
    public PayrollRunDetail run(@PathVariable Long id) {
        return payrollService.run(id);
    }

    @PostMapping("/api/payroll/runs/{id}/calculate")
    @PreAuthorize("hasAuthority('PAYROLL:EDIT')")
    public PayrollRunDetail calculate(@PathVariable Long id) {
        return payrollService.calculate(id);
    }

    @PostMapping("/api/payroll/runs/{id}/pay")
    @PreAuthorize("hasAuthority('PAYROLL:APPROVE')")
    public PayrollRunDetail payAll(@PathVariable Long id, @Valid @RequestBody SalaryPaymentRequest request) {
        return payrollService.payAll(id, request.getMethod(), request.getReference());
    }

    @PostMapping("/api/payslips/{id}/pay")
    @PreAuthorize("hasAuthority('PAYROLL:APPROVE')")
    public PayslipResponse paySlip(@PathVariable Long id, @Valid @RequestBody SalaryPaymentRequest request) {
        return payrollService.paySlip(id, request.getMethod(), request.getReference());
    }

    @GetMapping("/api/payslips/mine")
    @PreAuthorize("hasAuthority('PAYROLL:VIEW')")
    public List<PayslipResponse> mine() {
        return payrollService.myPayslips();
    }

    @GetMapping("/api/payslips/{id}")
    @PreAuthorize("hasAuthority('PAYROLL:VIEW')")
    public PayslipResponse payslip(@PathVariable Long id) {
        return payrollService.payslip(id);
    }

    @GetMapping("/api/payslips/{id}/pdf")
    @PreAuthorize("hasAuthority('PAYROLL:VIEW')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        PayslipResponse slip = payrollService.payslip(id);
        String name = "payslip-" + slip.employeeNo() + "-" + slip.year() + "-" + String.format("%02d", slip.month()) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(payrollService.payslipPdf(id));
    }
}
