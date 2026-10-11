package com.sitemaster.expense;

import com.sitemaster.budget.dto.BudgetCheckResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.ExpenseCategory;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.dailyreport.dto.ReviewDecisionRequest;
import com.sitemaster.expense.dto.ExpenseForm;
import com.sitemaster.expense.dto.ExpensePaymentRequest;
import com.sitemaster.expense.dto.ExpenseResponse;
import com.sitemaster.file.FileResponses;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Expense requests. */
@RestController
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping("/api/expenses")
    @PreAuthorize("hasAuthority('EXPENSE:VIEW')")
    public PageResponse<ExpenseResponse> list(@RequestParam(required = false) Long projectId,
                                              @RequestParam(required = false) RequestStatus status,
                                              @RequestParam(required = false) ExpenseCategory category,
                                              @RequestParam(defaultValue = "false") boolean mine,
                                              @Valid @ModelAttribute PageQuery pageQuery) {
        return expenseService.search(projectId, status, category, mine, pageQuery);
    }

    @GetMapping("/api/expenses/{id}")
    @PreAuthorize("hasAuthority('EXPENSE:VIEW')")
    public ExpenseResponse get(@PathVariable Long id) {
        return expenseService.get(id);
    }

    @PostMapping(value = "/api/projects/{projectId}/expenses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EXPENSE:CREATE')")
    public ExpenseResponse create(@PathVariable Long projectId, @Valid @ModelAttribute ExpenseForm form) {
        return expenseService.create(projectId, form);
    }

    @GetMapping("/api/expenses/{id}/budget-check")
    @PreAuthorize("hasAuthority('EXPENSE:APPROVE')")
    public BudgetCheckResponse budgetCheck(@PathVariable Long id) {
        return expenseService.budgetCheck(id);
    }

    @PostMapping("/api/expenses/{id}/approve")
    @PreAuthorize("hasAuthority('EXPENSE:APPROVE')")
    public ExpenseResponse approve(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return expenseService.approve(id, request.getRemarks(), request.isAcknowledgeOverrun());
    }

    @PostMapping("/api/expenses/{id}/reject")
    @PreAuthorize("hasAuthority('EXPENSE:APPROVE')")
    public ExpenseResponse reject(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return expenseService.reject(id, request.getRemarks());
    }

    @PostMapping("/api/expenses/{id}/payment")
    @PreAuthorize("hasAuthority('PAYMENT:CREATE')")
    public ExpenseResponse pay(@PathVariable Long id, @Valid @RequestBody ExpensePaymentRequest request) {
        return expenseService.pay(id, request);
    }

    @GetMapping("/api/expenses/{id}/receipt")
    @PreAuthorize("hasAuthority('EXPENSE:VIEW')")
    public ResponseEntity<Resource> receipt(@PathVariable Long id) {
        return FileResponses.attachment(expenseService.openReceipt(id));
    }
}
