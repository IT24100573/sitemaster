package com.sitemaster.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Data access for payroll runs. */
public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {

    boolean existsByYearAndMonth(int year, int month);

    Optional<PayrollRun> findByYearAndMonth(int year, int month);

    List<PayrollRun> findAllByOrderByYearDescMonthDesc();
}
