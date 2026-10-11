package com.sitemaster.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Data access for employee documents (always looked up within their employee). */
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {

    Optional<EmployeeDocument> findByIdAndEmployeeId(Long id, Long employeeId);

    long countByEmployeeId(Long employeeId);
}
