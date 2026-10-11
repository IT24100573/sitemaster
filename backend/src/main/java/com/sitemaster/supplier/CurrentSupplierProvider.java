package com.sitemaster.supplier;

import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.security.CurrentUserProvider;
import org.springframework.stereotype.Component;

/**
 * The Supplier record of the signed-in SUPPLIER user. Every supplier-portal
 * query is filtered by it, so a supplier never sees another supplier's data.
 * <p>
 * OOP concept: Encapsulation - one place maps the session user to a supplier.
 */
@Component
public class CurrentSupplierProvider {

    private final SupplierRepository supplierRepository;
    private final CurrentUserProvider currentUser;

    public CurrentSupplierProvider(SupplierRepository supplierRepository, CurrentUserProvider currentUser) {
        this.supplierRepository = supplierRepository;
        this.currentUser = currentUser;
    }

    public Supplier requireSupplier() {
        return supplierRepository.findByUserId(currentUser.requireId())
                .filter(Supplier::isActive)
                .orElseThrow(() -> new UnauthorizedActionException("supplier.notLinked"));
    }
}
