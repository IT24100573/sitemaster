package com.sitemaster.requests;

import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.enums.Urgency;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.materialrequest.MaterialRequest;
import com.sitemaster.project.Phase;
import com.sitemaster.project.Project;
import com.sitemaster.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.sitemaster.support.TestData.TODAY;
import static com.sitemaster.support.TestData.material;
import static com.sitemaster.support.TestData.phase;
import static com.sitemaster.support.TestData.project;
import static com.sitemaster.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** BaseRequest workflow and the MaterialRequest overrides (budget check, later steps). */
class MaterialRequestTest {

    private final User engineer = user(1, RoleName.SITE_ENGINEER);
    private final User pm = user(2, RoleName.PROJECT_MANAGER);
    private MaterialRequest request;

    @BeforeEach
    void setUp() {
        Project project = project(10, TODAY.minusDays(30), TODAY.plusDays(300), "1000000.00");
        Phase phase = phase(20, project, TODAY.minusDays(30), TODAY.plusDays(60), "500000.00");
        request = new MaterialRequest(project, phase, material(30, "2450.00"), new BigDecimal("100"), Urgency.HIGH,
                TODAY.plusDays(3), null, engineer);
    }

    @Test
    void estimatesCostFromCataloguePrice() {
        assertThat(request.getEstimatedCost()).isEqualByComparingTo("245000.00");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
    }

    @Test
    void approvesWithinBudget() {
        request.prepareBudgetCheck(new BigDecimal("300000.00"), false);
        request.approve(pm, "OK");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(request.getApprovedBy()).isEqualTo(pm);
    }

    @Test
    void overrunNeedsAcknowledgement() {
        request.prepareBudgetCheck(new BigDecimal("100000.00"), false);
        assertThatThrownBy(() -> request.approve(pm, null))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("messageKey").isEqualTo("materialRequest.budgetOverrun");

        request.prepareBudgetCheck(new BigDecimal("100000.00"), true);
        request.approve(pm, null);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
    }

    @Test
    void approveWithoutBudgetCheckIsAProgrammingError() {
        assertThatThrownBy(() -> request.approve(pm, null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void nobodyApprovesTheirOwnRequest() {
        request.prepareBudgetCheck(new BigDecimal("999999.00"), false);
        assertThatThrownBy(() -> request.approve(engineer, null)).isInstanceOf(UnauthorizedActionException.class);
    }

    @Test
    void rejectionNeedsARealReason() {
        assertThatThrownBy(() -> request.reject(pm, "no")).isInstanceOf(BusinessRuleException.class);
        request.reject(pm, "Use the stock already on site.");
        assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
    }

    @Test
    void decisionsHappenOnlyOnce() {
        request.reject(pm, "Not needed this week.");
        request.prepareBudgetCheck(new BigDecimal("999999.00"), false);
        assertThatThrownBy(() -> request.approve(pm, null)).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void followsOrderPaidDeliveredInOrder() {
        assertThatThrownBy(() -> request.markOrdered()).isInstanceOf(InvalidStatusTransitionException.class);
        request.prepareBudgetCheck(new BigDecimal("999999.00"), false);
        request.approve(pm, null);
        request.markOrdered();
        assertThatThrownBy(() -> request.confirmDelivery(BigDecimal.TEN, engineer, null, LocalDateTime.now()))
                .isInstanceOf(InvalidStatusTransitionException.class);
        request.markPaid();
        request.confirmDelivery(new BigDecimal("100"), engineer, "All received", LocalDateTime.now());
        assertThat(request.getStatus()).isEqualTo(RequestStatus.DELIVERED);
    }

    @Test
    void cannotConfirmMoreThanRequested() {
        request.prepareBudgetCheck(new BigDecimal("999999.00"), false);
        request.approve(pm, null);
        request.markOrdered();
        request.markPaid();
        assertThatThrownBy(() -> request.confirmDelivery(new BigDecimal("101"), engineer, null, LocalDateTime.now()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void declinedOrderReturnsRequestToApproved() {
        request.prepareBudgetCheck(new BigDecimal("999999.00"), false);
        request.approve(pm, null);
        request.markOrdered();
        request.reopenForOrdering();
        assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
    }
}
