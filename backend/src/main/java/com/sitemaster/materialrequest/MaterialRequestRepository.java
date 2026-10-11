package com.sitemaster.materialrequest;

import com.sitemaster.common.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;

/** Data access for material requests. */
public interface MaterialRequestRepository
        extends JpaRepository<MaterialRequest, Long>, JpaSpecificationExecutor<MaterialRequest> {

    @Query("select coalesce(sum(r.estimatedCost), 0) from MaterialRequest r "
            + "where r.phase.id = :phaseId and r.status in :statuses and (:excludeId is null or r.id <> :excludeId)")
    BigDecimal sumCost(@Param("phaseId") Long phaseId, @Param("statuses") Collection<RequestStatus> statuses,
                       @Param("excludeId") Long excludeId);

    long countByProjectIdAndStatusIn(Long projectId, Collection<RequestStatus> statuses);
}
