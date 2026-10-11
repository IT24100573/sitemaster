package com.sitemaster.pettycash;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.expense.ExpenseRequest;
import com.sitemaster.pettycash.dto.AdjustmentRequest;
import com.sitemaster.pettycash.dto.FundRequest;
import com.sitemaster.pettycash.dto.FundResponse;
import com.sitemaster.pettycash.dto.TopUpRequest;
import com.sitemaster.pettycash.dto.TransactionResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Project petty cash: the Accountant sets up and tops up a float; approved
 * expenses can be paid from it; cash counts are recorded as adjustments.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface PettyCashService {

    List<FundResponse> funds();

    FundResponse fund(Long projectId);

    FundResponse saveFund(Long projectId, FundRequest request);

    FundResponse topUp(Long projectId, TopUpRequest request);

    FundResponse adjust(Long projectId, AdjustmentRequest request);

    PageResponse<TransactionResponse> transactions(Long projectId, PageQuery pageQuery);

    /** Pays an approved expense out of its project's fund (used by the expense service). */
    void spendForExpense(ExpenseRequest expense, LocalDate date);
}
