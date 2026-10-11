package com.sitemaster.attendance;

import com.sitemaster.common.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/** Data access for overtime. */
public interface OvertimeRepository extends JpaRepository<Overtime, Long>, JpaSpecificationExecutor<Overtime> {

    Optional<Overtime> findByAttendanceId(Long attendanceId);

    /** Approved overtime on verified attendance only, as payroll requires. */
    @Query("select coalesce(sum(o.hours), 0) from Overtime o where o.attendance.employee.id = :employeeId "
            + "and o.status = :status and o.attendance.verified = true "
            + "and o.attendance.date between :from and :to")
    BigDecimal sumHours(@Param("employeeId") Long employeeId, @Param("status") RequestStatus status,
                        @Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByStatus(RequestStatus status);

    long countByStatusAndAttendanceDateBetween(RequestStatus status, LocalDate from, LocalDate to);
}
