package com.sitemaster.supplier;

import com.sitemaster.common.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for supplier applications. */
public interface SupplierApplicationRepository
        extends JpaRepository<SupplierApplication, Long>, JpaSpecificationExecutor<SupplierApplication> {

    boolean existsByEmailIgnoreCaseAndStatus(String email, ApplicationStatus status);

    boolean existsByRegistrationNoIgnoreCaseAndStatus(String registrationNo, ApplicationStatus status);

    long countByStatus(ApplicationStatus status);
}
