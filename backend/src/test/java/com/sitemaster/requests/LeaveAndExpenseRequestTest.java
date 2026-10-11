package com.sitemaster.requests;

import com.sitemaster.common.enums.ExpenseCategory;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.employee.Employee;
import com.sitemaster.expense.ExpenseRequest;
import com.sitemaster.leave.LeaveBalance;
import com.sitemaster.leave.LeaveRequest;
import com.sitemaster.leave.LeaveType;
import com.sitemaster.project.Phase;
import com.sitemaster.project.Project;
import com.sitemaster.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.sitemaster.support.TestData.TODAY;
import static com.sitemaster.support.TestData.dailyWage;
import static com.sitemaster.support.TestData.phase;
import static com.sitemaster.support.TestData.project;
import static com.sitemaster.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Polymorphism: LeaveRequest and ExpenseRequest override approve() with their own rules. */
class LeaveAndExpenseRequestTest {

    private final User hr = user(5, RoleName.HR_MANAGER);

    private LeaveType annual() {
        LeaveType type = new LeaveType("ANNUAL");
        type.update("Annual leave", null, true, 0, true);
        return type;
    }

    @Test
    void approvingLeaveDeductsTheBalance() {
        Employee worker = dailyWage(1, "3000.00");
        LeaveBalance balance = new LeaveBalance(worker, annual(), 2026, new BigDecimal("14"));
        LeaveRequest request = new LeaveRequest(worker, annual(), TODAY, TODAY.plusDays(2), new BigDecimal("3"),
                "Family event", worker.getUser());
        request.prepareBalance(balance);
        request.approve(hr, null);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(balance.remaining()).isEqualByComparingTo("11");
    }

    @Test
    void leaveCannotExceedTheBalance() {
        Employee worker = dailyWage(1, "3000.00");
        LeaveBalance balance = new LeaveBalance(worker, annual(), 2026, new BigDecimal("2"));
        LeaveRequest request = new LeaveRequest(worker, annual(), TODAY, TODAY.plusDays(2), new BigDecimal("3"),
                "Family event", worker.getUser());
        request.prepareBalance(balance);
        assertThatThrownBy(() -> request.approve(hr, null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void ownLeaveCannotBeApprovedAndBalanceStaysUntouched() {
        Employee worker = dailyWage(1, "3000.00");
        LeaveBalance balance = new LeaveBalance(worker, annual(), 2026, new BigDecimal("14"));
        LeaveRequest request = new LeaveRequest(worker, annual(), TODAY, TODAY, BigDecimal.ONE, "Bank visit",
                worker.getUser());
        request.prepareBalance(balance);
        assertThatThrownBy(() -> request.approve(worker.getUser(), null)).isInstanceOf(RuntimeException.class);
        assertThat(balance.remaining()).isEqualByComparingTo("14");
    }

    private ExpenseRequest expense(String amount) {
        Project project = project(10, TODAY.minusDays(30), TODAY.plusDays(300), "1000000.00");
        Phase phase = phase(20, project, TODAY.minusDays(30), TODAY.plusDays(60), "100000.00");
        return new ExpenseRequest(project, phase, ExpenseCategory.LABOUR, new BigDecimal(amount), TODAY, "Crew",
                "Scaffolding crew", user(1, RoleName.SITE_ENGINEER));
    }

    @Test
    void receiptIsRequiredAboveThreshold() {
        assertThatThrownBy(() -> expense("6000.00").requireReceipt(new BigDecimal("5000.00")))
                .isInstanceOf(BusinessRuleException.class);
        expense("4500.00").requireReceipt(new BigDecimal("5000.00"));
    }

    @Test
    void expenseIsPaidOnlyAfterApproval() {
        ExpenseRequest expense = expense("4500.00");
        assertThatThrownBy(() -> expense.markPaid(LocalDateTime.now())).isInstanceOf(InvalidStatusTransitionException.class);
        expense.prepareBudgetCheck(new BigDecimal("50000.00"), false);
        expense.approve(user(2, RoleName.PROJECT_MANAGER), null);
        expense.markPaid(LocalDateTime.now());
        assertThat(expense.getStatus()).isEqualTo(RequestStatus.PAID);
        assertThatThrownBy(() -> expense.markPaid(LocalDateTime.now())).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void expenseOverrunNeedsAcknowledgement() {
        ExpenseRequest expense = expense("4500.00");
        expense.prepareBudgetCheck(new BigDecimal("1000.00"), false);
        assertThatThrownBy(() -> expense.approve(user(2, RoleName.PROJECT_MANAGER), null))
                .isInstanceOf(BusinessRuleException.class);
    }
}
