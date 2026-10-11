package com.sitemaster.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

/** Data access for phases. */
public interface PhaseRepository extends JpaRepository<Phase, Long> {

    List<Phase> findByProjectIdOrderBySortOrderAscStartDateAsc(Long projectId);

    @Query("select coalesce(sum(p.budget), 0) from Phase p where p.project.id = :projectId "
            + "and (:excludeId is null or p.id <> :excludeId)")
    BigDecimal sumBudget(@Param("projectId") Long projectId, @Param("excludeId") Long excludeId);
}
