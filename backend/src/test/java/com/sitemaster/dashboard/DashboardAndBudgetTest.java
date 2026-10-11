package com.sitemaster.dashboard;

import com.sitemaster.budget.PhaseBudgetService;
import com.sitemaster.budget.PhaseCostSource;
import com.sitemaster.budget.dto.BudgetCheckResponse;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.project.Phase;
import com.sitemaster.project.Project;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.sitemaster.support.TestData.TODAY;
import static com.sitemaster.support.TestData.phase;
import static com.sitemaster.support.TestData.project;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardAndBudgetTest {

    private static RoleDashboard dashboardFor(RoleName role) {
        RoleDashboard dashboard = mock(RoleDashboard.class);
        when(dashboard.role()).thenReturn(role);
        return dashboard;
    }

    @Test
    void factoryReturnsTheDashboardForEachRole() {
        RoleDashboard admin = dashboardFor(RoleName.ADMIN);
        RoleDashboard worker = dashboardFor(RoleName.WORKER);
        DashboardFactory factory = new DashboardFactory(List.of(admin, worker));
        assertThat(factory.create(RoleName.ADMIN)).isSameAs(admin);
        assertThat(factory.create(RoleName.WORKER)).isSameAs(worker);
        assertThatThrownBy(() -> factory.create(RoleName.HR_MANAGER)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void budgetCheckAddsUpEveryCostSource() {
        Project project = project(1, TODAY.minusDays(10), TODAY.plusDays(100), "1000000.00");
        Phase phase = phase(2, project, TODAY.minusDays(10), TODAY.plusDays(50), "500000.00");
        PhaseCostSource materials = mock(PhaseCostSource.class);
        PhaseCostSource expenses = mock(PhaseCostSource.class);
        when(materials.committedCost(anyLong(), any())).thenReturn(new BigDecimal("300000.00"));
        when(expenses.committedCost(anyLong(), any())).thenReturn(new BigDecimal("150000.00"));
        PhaseBudgetService service = new PhaseBudgetService(List.of(materials, expenses));

        BudgetCheckResponse within = service.check(phase, new BigDecimal("50000.00"), null);
        assertThat(within.remaining()).isEqualByComparingTo("50000.00");
        assertThat(within.overrun()).isFalse();

        BudgetCheckResponse over = service.check(phase, new BigDecimal("50000.01"), null);
        assertThat(over.overrun()).isTrue();
    }
}
