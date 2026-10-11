package com.sitemaster.attendance;

import com.sitemaster.common.enums.AttendanceStatus;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.InvalidStatusTransitionException;
import com.sitemaster.employee.Employee;
import com.sitemaster.project.Project;
import com.sitemaster.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.sitemaster.support.TestData.TODAY;
import static com.sitemaster.support.TestData.dailyWage;
import static com.sitemaster.support.TestData.project;
import static com.sitemaster.support.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Attendance time rules and overtime limits. */
class AttendanceTest {

    private final Employee worker = dailyWage(1, "3000.00");
    private final Project site = project(10, TODAY.minusDays(30), TODAY.plusDays(300), "1000000.00");
    private final User engineer = user(2, RoleName.SITE_ENGINEER);

    @Test
    void checkInThenCheckOutComputesHours() {
        Attendance a = Attendance.checkIn(worker, site, TODAY.atTime(8, 0), worker.getUser());
        a.checkOut(TODAY.atTime(17, 30));
        assertThat(a.getHoursWorked()).isEqualByComparingTo("9.50");
        assertThat(a.isVerified()).isFalse();
    }

    @Test
    void cannotCheckOutTwice() {
        Attendance a = Attendance.checkIn(worker, site, TODAY.atTime(8, 0), worker.getUser());
        a.checkOut(TODAY.atTime(17, 0));
        assertThatThrownBy(() -> a.checkOut(TODAY.atTime(18, 0))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void checkOutMustBeAfterCheckInAndWithinSixteenHours() {
        Attendance a = new Attendance(worker, site, TODAY, engineer);
        assertThatThrownBy(() -> a.record(site, null, AttendanceStatus.PRESENT, TODAY.atTime(17, 0),
                TODAY.atTime(8, 0), null, engineer)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> a.record(site, null, AttendanceStatus.PRESENT, TODAY.atTime(5, 0),
                TODAY.atTime(22, 0), null, engineer)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void correctionClearsVerification() {
        Attendance a = new Attendance(worker, site, TODAY, engineer);
        a.record(site, null, AttendanceStatus.PRESENT, TODAY.atTime(8, 0), TODAY.atTime(17, 0), null, engineer);
        a.verify(engineer, LocalDateTime.now());
        a.record(site, null, AttendanceStatus.HALF_DAY, TODAY.atTime(8, 0), TODAY.atTime(12, 0), null, engineer);
        assertThat(a.isVerified()).isFalse();
    }

    @Test
    void absenceClearsTimes() {
        Attendance a = new Attendance(worker, site, TODAY, engineer);
        a.record(site, null, AttendanceStatus.ABSENT, TODAY.atTime(8, 0), TODAY.atTime(17, 0), null, engineer);
        assertThat(a.getCheckIn()).isNull();
        assertThat(a.getHoursWorked()).isEqualByComparingTo("0");
    }

    @Test
    void overtimeIsLimitedToEightHoursOnAWorkingDay() {
        Attendance present = new Attendance(worker, site, TODAY, engineer);
        present.record(site, null, AttendanceStatus.PRESENT, TODAY.atTime(8, 0), TODAY.atTime(19, 0), null, engineer);
        Overtime overtime = new Overtime(present, engineer);
        assertThatThrownBy(() -> overtime.update(new BigDecimal("9"), null, engineer)).isInstanceOf(BusinessRuleException.class);
        overtime.update(new BigDecimal("2"), "Pour", engineer);
        overtime.decide(true, user(3, RoleName.HR_MANAGER), null, LocalDateTime.now());
        assertThat(overtime.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThatThrownBy(() -> overtime.update(new BigDecimal("3"), null, engineer))
                .isInstanceOf(InvalidStatusTransitionException.class);

        Attendance absent = new Attendance(worker, site, TODAY.minusDays(1), engineer);
        absent.record(site, null, AttendanceStatus.ABSENT, null, null, null, engineer);
        assertThatThrownBy(() -> new Overtime(absent, engineer).update(BigDecimal.ONE, null, engineer))
                .isInstanceOf(BusinessRuleException.class);
    }
}
