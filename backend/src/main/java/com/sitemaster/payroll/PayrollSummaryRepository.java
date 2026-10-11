package com.sitemaster.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data access for HR's payroll summaries. */
public interface PayrollSummaryRepository extends JpaRepository<PayrollSummary, Long> {

    @Query("select s from PayrollSummary s join fetch s.employee e join fetch e.user "
            + "where s.payrollRun.id = :runId order by e.employeeNo")
    List<PayrollSummary> findForRun(@Param("runId") Long runId);
}
