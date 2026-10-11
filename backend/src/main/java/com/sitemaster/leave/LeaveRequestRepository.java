package com.sitemaster.leave;

import com.sitemaster.common.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/** Data access for leave requests. */
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long>, JpaSpecificationExecutor<LeaveRequest> {

    /** Requests of the employee in the given statuses that overlap the period. */
    @Query("select count(r) > 0 from LeaveRequest r where r.employee.id = :employeeId and r.status in :statuses "
            + "and r.fromDate <= :to and r.toDate >= :from")
    boolean existsOverlap(@Param("employeeId") Long employeeId, @Param("from") LocalDate from,
                          @Param("to") LocalDate to, @Param("statuses") Collection<RequestStatus> statuses);

    @Query("select coalesce(sum(r.days), 0) from LeaveRequest r where r.employee.id = :employeeId "
            + "and r.leaveType.id = :typeId and r.status = com.sitemaster.common.enums.RequestStatus.PENDING "
            + "and year(r.fromDate) = :year")
    BigDecimal pendingDays(@Param("employeeId") Long employeeId, @Param("typeId") Long typeId, @Param("year") int year);

    @Query("select r from LeaveRequest r join fetch r.leaveType where r.employee.id = :employeeId "
            + "and r.status = com.sitemaster.common.enums.RequestStatus.APPROVED "
            + "and r.fromDate <= :to and r.toDate >= :from")
    List<LeaveRequest> findApprovedOverlapping(@Param("employeeId") Long employeeId, @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    long countByStatus(RequestStatus status);

    @Query("select count(r) from LeaveRequest r where r.status = com.sitemaster.common.enums.RequestStatus.PENDING "
            + "and r.fromDate <= :to and r.toDate >= :from")
    long countPendingOverlapping(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
