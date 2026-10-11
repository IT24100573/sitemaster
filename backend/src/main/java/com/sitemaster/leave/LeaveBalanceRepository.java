package com.sitemaster.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for leave balances. */
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeIdAndYear(Long employeeId, Long leaveTypeId, int year);
}
