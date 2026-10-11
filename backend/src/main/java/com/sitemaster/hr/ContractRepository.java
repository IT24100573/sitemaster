package com.sitemaster.hr;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/** Data access for contracts. */
public interface ContractRepository extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {

    boolean existsByContractNoIgnoreCase(String contractNo);

    boolean existsByContractNoIgnoreCaseAndIdNot(String contractNo, Long id);

    @Query("select c from Contract c join fetch c.employee e join fetch e.user "
            + "where c.reminderSentAt is null and c.endDate between :from and :to")
    List<Contract> findDueForReminder(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByEndDateBetween(LocalDate from, LocalDate to);
}
