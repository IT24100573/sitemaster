package com.sitemaster.pettycash;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for petty cash movements. */
public interface PettyCashTransactionRepository extends JpaRepository<PettyCashTransaction, Long> {

    Page<PettyCashTransaction> findByFundId(Long fundId, Pageable pageable);

    Optional<PettyCashTransaction> findFirstByExpenseRequestId(Long expenseRequestId);
}
