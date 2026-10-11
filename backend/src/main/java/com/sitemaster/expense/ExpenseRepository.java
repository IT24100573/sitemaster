package com.sitemaster.expense;

import com.sitemaster.common.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/** Data access for expense requests. */
public interface ExpenseRepository extends JpaRepository<ExpenseRequest, Long>, JpaSpecificationExecutor<ExpenseRequest> {

    @Query("select coalesce(sum(e.amount), 0) from ExpenseRequest e where e.phase.id = :phaseId and e.status in :statuses")
    BigDecimal sumForPhase(@Param("phaseId") Long phaseId, @Param("statuses") Collection<RequestStatus> statuses);

    @Query("select coalesce(sum(e.amount), 0) from ExpenseRequest e where e.project.id = :projectId and e.status in :statuses")
    BigDecimal sumForProject(@Param("projectId") Long projectId, @Param("statuses") Collection<RequestStatus> statuses);

    /** [category, total] of paid expenses of a project. */
    @Query("select e.category, coalesce(sum(e.amount), 0) from ExpenseRequest e where e.project.id = :projectId "
            + "and e.status = com.sitemaster.common.enums.RequestStatus.PAID group by e.category")
    List<Object[]> paidByCategory(@Param("projectId") Long projectId);

    long countByProjectIdAndStatusIn(Long projectId, Collection<RequestStatus> statuses);
}
