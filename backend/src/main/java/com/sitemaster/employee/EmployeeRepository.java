package com.sitemaster.employee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Data access for employees. */
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    boolean existsByNic(String nic);

    boolean existsByNicAndIdNot(String nic, Long id);

    boolean existsByUserId(Long userId);

    Optional<Employee> findByUserId(Long userId);

    List<Employee> findByUserIdIn(Collection<Long> userIds);

    /** Employees with an active account who had joined by the given date (payroll candidates). */
    @Query("select e from Employee e join fetch e.user u "
            + "where u.active = true and e.joinDate <= :date order by e.employeeNo")
    List<Employee> findPayable(@Param("date") LocalDate date);

    @Query(value = "select nextval('employee_number_seq')", nativeQuery = true)
    long nextEmployeeNumber();
}
