package com.sitemaster.attendance;

import com.sitemaster.common.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Data access for attendance. */
public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {

    Optional<Attendance> findByEmployeeIdAndDate(Long employeeId, LocalDate date);

    boolean existsByEmployeeIdAndDate(Long employeeId, LocalDate date);

    @Query("select a from Attendance a join fetch a.project where a.employee.id = :employeeId "
            + "and a.date between :from and :to order by a.date")
    List<Attendance> findForEmployee(@Param("employeeId") Long employeeId, @Param("from") LocalDate from,
                                     @Param("to") LocalDate to);

    @Query("select a from Attendance a join fetch a.employee e join fetch e.user "
            + "where a.date between :from and :to and (:projectId is null or a.project.id = :projectId)")
    List<Attendance> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                 @Param("projectId") Long projectId);

    @Query("select count(a) from Attendance a where a.employee.id = :employeeId and a.verified = true "
            + "and a.status = :status and a.date between :from and :to")
    long countVerified(@Param("employeeId") Long employeeId, @Param("status") AttendanceStatus status,
                       @Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByVerifiedFalseAndDateBetween(LocalDate from, LocalDate to);

    long countByDateAndStatusIn(LocalDate date, List<AttendanceStatus> statuses);
}
