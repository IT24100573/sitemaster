package com.sitemaster.material;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Data access for site stock. */
public interface SiteStockRepository extends JpaRepository<SiteStock, Long> {

    Optional<SiteStock> findByProjectIdAndMaterialId(Long projectId, Long materialId);

    @Query("select s from SiteStock s join fetch s.material m where s.project.id = :projectId order by m.name")
    List<SiteStock> findForProject(@Param("projectId") Long projectId);
}
