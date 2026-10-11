package com.sitemaster.pettycash;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** Data access for petty cash funds. */
public interface PettyCashFundRepository extends JpaRepository<PettyCashFund, Long>, JpaSpecificationExecutor<PettyCashFund> {

    Optional<PettyCashFund> findByProjectId(Long projectId);

    boolean existsByProjectId(Long projectId);
}
