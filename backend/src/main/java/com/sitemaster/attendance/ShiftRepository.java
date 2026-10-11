package com.sitemaster.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for shifts. */
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    List<Shift> findAllByOrderByStartTimeAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
