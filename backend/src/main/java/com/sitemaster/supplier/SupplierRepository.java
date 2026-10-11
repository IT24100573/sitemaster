package com.sitemaster.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/** Data access for suppliers. */
public interface SupplierRepository extends JpaRepository<Supplier, Long>, JpaSpecificationExecutor<Supplier> {

    boolean existsByRegistrationNoIgnoreCase(String registrationNo);

    Optional<Supplier> findByUserId(Long userId);
}
