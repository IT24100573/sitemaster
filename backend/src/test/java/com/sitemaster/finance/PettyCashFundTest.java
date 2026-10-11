package com.sitemaster.finance;

import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.pettycash.PettyCashFund;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.sitemaster.support.TestData.TODAY;
import static com.sitemaster.support.TestData.project;
import static com.sitemaster.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Encapsulation: the fund protects its own balance. */
class PettyCashFundTest {

    private PettyCashFund fund;

    @BeforeEach
    void setUp() {
        fund = new PettyCashFund(project(1, TODAY.minusDays(10), TODAY.plusDays(100), "1000000.00"));
        fund.configure(user(2, RoleName.SITE_ENGINEER), new BigDecimal("50000.00"), true);
    }

    @Test
    void topUpAndSpend() {
        fund.topUp(new BigDecimal("30000.00"));
        fund.spend(new BigDecimal("4500.00"));
        assertThat(fund.getBalance()).isEqualByComparingTo("25500.00");
    }

    @Test
    void topUpCannotPassTheFloatLimit() {
        fund.topUp(new BigDecimal("45000.00"));
        assertThatThrownBy(() -> fund.topUp(new BigDecimal("6000.00"))).isInstanceOf(BusinessRuleException.class);
        assertThat(fund.getBalance()).isEqualByComparingTo("45000.00");
    }

    @Test
    void cannotSpendMoreThanTheBalance() {
        fund.topUp(new BigDecimal("1000.00"));
        assertThatThrownBy(() -> fund.spend(new BigDecimal("1000.01"))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cashCountAdjustsAndReportsTheDifference() {
        fund.topUp(new BigDecimal("10000.00"));
        assertThat(fund.adjustTo(new BigDecimal("9750.00"))).isEqualByComparingTo("-250.00");
        assertThat(fund.getBalance()).isEqualByComparingTo("9750.00");
    }

    @Test
    void inactiveFundCannotBeUsed() {
        fund.configure(user(2, RoleName.SITE_ENGINEER), new BigDecimal("50000.00"), false);
        assertThatThrownBy(() -> fund.topUp(BigDecimal.TEN)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void limitCannotDropBelowTheCurrentBalance() {
        fund.topUp(new BigDecimal("20000.00"));
        assertThatThrownBy(() -> fund.configure(user(2, RoleName.SITE_ENGINEER), new BigDecimal("10000.00"), true))
                .isInstanceOf(BusinessRuleException.class);
    }
}
