package com.sitemaster.expense;

import com.sitemaster.budget.dto.BudgetCheckResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.ExpenseCategory;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.expense.dto.ExpenseForm;
import com.sitemaster.expense.dto.ExpensePaymentRequest;
import com.sitemaster.expense.dto.ExpenseResponse;
import com.sitemaster.file.FileDownload;

/**
 * Expense requests: raised on site, approved by the project's PM, paid by the
 * Accountant (by bank, cheque, cash, or from the project's petty cash).
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface ExpenseService {

    PageResponse<ExpenseResponse> search(Long projectId, RequestStatus status, ExpenseCategory category,
                                         boolean mineOnly, PageQuery pageQuery);

    ExpenseResponse get(Long id);

    ExpenseResponse create(Long projectId, ExpenseForm form);

    BudgetCheckResponse budgetCheck(Long id);

    ExpenseResponse approve(Long id, String remarks, boolean acknowledgeOverrun);

    ExpenseResponse reject(Long id, String remarks);

    ExpenseResponse pay(Long id, ExpensePaymentRequest request);

    FileDownload openReceipt(Long id);
}
