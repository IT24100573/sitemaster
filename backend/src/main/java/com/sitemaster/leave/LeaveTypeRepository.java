package com.sitemaster.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for leave types. */
public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {

    List<LeaveType> findAllByOrderByNameAsc();

    List<LeaveType> findByActiveTrueOrderByNameAsc();

    boolean existsByCodeIgnoreCase(String code);
}
