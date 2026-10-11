package com.sitemaster.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** Data access for payslips. */
public interface PayslipRepository extends JpaRepository<Payslip, Long> {

    @Query("select p from Payslip p join fetch p.employee e join fetch e.user "
            + "where p.payrollRun.id = :runId order by e.employeeNo")
    List<Payslip> findForRun(@Param("runId") Long runId);

    @Query("select p from Payslip p join fetch p.payrollRun r where p.employee.id = :employeeId "
            + "order by r.year desc, r.month desc")
    List<Payslip> findForEmployee(@Param("employeeId") Long employeeId);

    long countByPayrollRunIdAndPaidFalse(Long runId);

    @Modifying
    @Query("delete from Payslip p where p.payrollRun.id = :runId")
    void deleteForRun(@Param("runId") Long runId);
}
